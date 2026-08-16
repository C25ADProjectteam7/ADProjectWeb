package com.expensehub.webbackend.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA {@link AttributeConverter} that transparently encrypts String fields
 * when writing to the database and decrypts them when reading.
 * <p>
 * Applied via {@code @Convert(converter = EncryptedStringConverter.class)} on
 * individual entity fields. The underlying {@link EncryptionService} is wired
 * through a static delegate because JPA converters are instantiated by
 * Hibernate, not the Spring container — see
 * {@code WebBackendApplication.initEncryptionConverter()}.
 * <p>
 * AES-256-GCM with a random IV ensures non-deterministic encryption:
 * identical plaintexts produce different ciphertexts.
 */
@Converter(autoApply = false)
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static EncryptionService encryptionService;

    /**
     * Called once by Spring after the {@link EncryptionService} bean is available
     * (typically from a {@code @PostConstruct} in the application main class).
     */
    public static void setEncryptionService(EncryptionService service) {
        encryptionService = service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return encryptionService.decrypt(dbData);
        } catch (Exception e) {
            // Graceful fallback for existing plaintext data during migration:
            // if the value can't be decrypted (e.g. it's legacy plaintext),
            // return it as-is so the migration job can re-save and encrypt it.
            return dbData;
        }
    }
}