package com.frist.assesspro.service;

import com.frist.assesspro.config.MinioProperties;
import com.frist.assesspro.dto.material.MaterialDTO;
import com.frist.assesspro.dto.material.SectionDTO;
import com.frist.assesspro.entity.Material;
import com.frist.assesspro.entity.Section;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.mapper.MaterialMapper;
import com.frist.assesspro.mapper.SectionMapper;
import com.frist.assesspro.repository.MaterialRepository;
import com.frist.assesspro.repository.SectionRepository;
import com.frist.assesspro.repository.TestRepository;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialService {

    private final SectionRepository sectionRepository;
    private final MaterialRepository materialRepository;
    private final TestRepository testRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final SectionMapper sectionMapper;
    private final MaterialMapper materialMapper;

    // --------------------------------------------------
    // 1. Получение секций
    // --------------------------------------------------

    /**
     * Все секции (включая неактивные) – для панели управления создателя.
     */
    @Transactional(readOnly = true)
    public List<SectionDTO> getAllSections() {
        return sectionRepository.findAllByOrderByOrderIndex().stream()
                .map(this::toSectionDtoWithComputedFields)
                .collect(Collectors.toList());
    }

    /**
     * Только активные секции – для публичной страницы.
     */
    @Transactional(readOnly = true)
    public List<SectionDTO> getAllActiveSections() {
        return sectionRepository.findAllByActiveTrueOrderByOrderIndex().stream()
                .map(this::toSectionDtoWithComputedFields)
                .collect(Collectors.toList());
    }

    // --------------------------------------------------
    // 2. CRUD секций
    // --------------------------------------------------

    /**
     * Создать новую секцию.
     */
    @Transactional
    public SectionDTO createSection(String title, String description) {
        int maxOrder = sectionRepository.getMaxOrderIndex();
        Section section = new Section();
        section.setTitle(title);
        section.setDescription(description);
        section.setOrderIndex(maxOrder + 1);
        section.setActive(true);
        section = sectionRepository.save(section);
        log.info("Создана секция '{}' (ID={})", section.getTitle(), section.getId());
        return toSectionDtoWithComputedFields(section);
    }

    /**
     * Обновить секцию (название, описание, активность).
     */
    @Transactional
    public SectionDTO updateSection(Long id, String title, String description, Boolean active) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Секция не найдена"));
        if (title != null && !title.isBlank()) {
            section.setTitle(title);
        }
        if (description != null) {
            section.setDescription(description);
        }
        if (active != null) {
            section.setActive(active);
        }
        section = sectionRepository.save(section);
        log.info("Обновлена секция '{}' (ID={})", section.getTitle(), section.getId());
        return toSectionDtoWithComputedFields(section);
    }

    /**
     * Удалить секцию (каскадно удалит материалы и отвяжет тесты).
     */
    @Transactional
    public void deleteSection(Long id) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Секция не найдена"));
        // Удаляем файлы из MinIO для всех материалов секции
        for (Material material : section.getMaterials()) {
            removeFromMinio(material.getObjectKey());
        }
        sectionRepository.delete(section);
        log.info("Удалена секция '{}' (ID={})", section.getTitle(), section.getId());
    }

    // --------------------------------------------------
    // 3. Управление PDF-материалами
    // --------------------------------------------------

    /**
     * Загрузить PDF в секцию.
     */
    @Transactional
    public MaterialDTO uploadPdfToSection(Long sectionId, MultipartFile file) throws Exception {
        // Проверки
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл пуст");
        }
        if (file.getSize() > 10_000_000) {
            throw new IllegalArgumentException("Размер файла превышает 10 МБ");
        }
        if (!"application/pdf".equals(file.getContentType())) {
            throw new IllegalArgumentException("Разрешены только PDF-файлы");
        }

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Секция не найдена"));

        ensureBucketExists();

        // Генерируем уникальный ключ
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectKey = UUID.randomUUID().toString() + extension;

        // Загружаем в MinIO
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(objectKey)
                        .stream(file.getInputStream(), file.getSize(), (long) -1) // partSize = -1 (значение по умолчанию)
                        .contentType("application/pdf")
                        .build()
        );
        log.info("Файл '{}' загружен в MinIO с ключом {}", originalFilename, objectKey);

        // Сохраняем метаданные
        Material material = Material.builder()
                .section(section)
                .fileName(originalFilename != null ? originalFilename : "document.pdf")
                .contentType("application/pdf")
                .fileSize(file.getSize())
                .objectKey(objectKey)
                .build();
        material = materialRepository.save(material);
        log.info("Создан материал ID={} для секции '{}'", material.getId(), section.getTitle());

        return sectionMapper.toDto(material);
    }

    /**
     * Получить материал по ID (сущность) – для получения имени файла и т.п.
     */
    @Transactional(readOnly = true)
    public Material getMaterialById(Long materialId) {
        return materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Материал не найден"));
    }

    /**
     * Получить входной поток PDF-файла для скачивания.
     */
    public InputStream getPdfInputStream(Long materialId) throws Exception {
        Material material = getMaterialById(materialId);
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(material.getObjectKey())
                        .build()
        );
    }

    /**
     * Удалить PDF-файл (из БД и MinIO).
     */
    @Transactional
    public void deleteMaterial(Long materialId) {
        Material material = getMaterialById(materialId);
        removeFromMinio(material.getObjectKey());
        materialRepository.delete(material);
        log.info("Материал ID={} удалён", materialId);
    }

    // --------------------------------------------------
    // 4. Привязка и отвязка тестов
    // --------------------------------------------------

    /**
     * Привязать тесты к секции (замена существующих связей).
     */
    @Transactional
    public void attachTestsToSection(Long sectionId, List<Long> testIds) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Секция не найдена"));
        List<Test> tests = testRepository.findAllById(testIds);
        section.getTests().clear(); // или заменить полностью, зависит от требований
        section.getTests().addAll(tests);
        sectionRepository.save(section);
        log.info("К секции '{}' привязано тестов: {}", section.getTitle(), tests.size());
    }

    /**
     * Отвязать один тест от секции.
     */
    @Transactional
    public void detachTestFromSection(Long sectionId, Long testId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Секция не найдена"));
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Тест не найден"));
        section.getTests().remove(test);
        sectionRepository.save(section);
        log.info("Тест '{}' отвязан от секции '{}'", test.getTitle(), section.getTitle());
    }

    // --------------------------------------------------
    // 5. Вспомогательные приватные методы
    // --------------------------------------------------

    /**
     * Преобразование сущности в DTO с учётом вычисляемых полей (hasPdf).
     * MapStruct выполняет основное маппирование, а мы дополняем вычисляемым полем.
     */
    private SectionDTO toSectionDtoWithComputedFields(Section section) {
        SectionDTO dto = sectionMapper.toDto(section);
        dto.setHasPdf(!section.getMaterials().isEmpty());
        return dto;
    }

    /**
     * Создать бакет, если его нет.
     */
    private void ensureBucketExists() throws Exception {
        String bucket = minioProperties.getBucketName();
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build()
        );
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucket).build()
            );
            log.info("Создан бакет MinIO: {}", bucket);
        }
    }

    /**
     * Удалить объект из MinIO (ошибки логируются, но не прерывают транзакцию).
     */
    private void removeFromMinio(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectKey)
                            .build()
            );
            log.info("Объект MinIO удалён: {}", objectKey);
        } catch (Exception e) {
            log.error("Ошибка удаления объекта MinIO: {}", objectKey, e);
        }
    }

    /**
     * Добавить видео
     */

    @Transactional
    public MaterialDTO uploadVideoToSection(Long sectionId, MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл пуст");
        }
        if (file.getSize() > 500_000_000) { // 500 МБ
            throw new IllegalArgumentException("Размер файла превышает 500 МБ");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new IllegalArgumentException("Разрешены только видеофайлы");
        }

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Секция не найдена"));

        ensureBucketExists();

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectKey = UUID.randomUUID().toString() + extension;

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(objectKey)
                        .stream(file.getInputStream(), file.getSize(), (long)-1)
                        .contentType(contentType)
                        .build()
        );

        Material material = Material.builder()
                .section(section)
                .fileName(originalFilename != null ? originalFilename : "video.mp4")
                .contentType(contentType)
                .fileSize(file.getSize())
                .objectKey(objectKey)
                .type(Material.MaterialType.VIDEO_FILE)
                .orderIndex(getNextOrderIndex(sectionId))
                .build();
        material = materialRepository.save(material);
        return materialMapper.toDto(material);
    }

    // Приватный метод для получения следующего orderIndex
    private int getNextOrderIndex(Long sectionId) {
        return materialRepository.findMaxOrderIndexBySectionId(sectionId).orElse(0) + 1;
    }
}
