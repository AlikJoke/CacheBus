package net.cache.bus.core.configuration;

/**
 * Тип распределенного кэша.
 *
 * @author Alik
 */
public enum CacheType {

    /**
     * Инвалидационный кэш
     */
    INVALIDATED(false),

    /**
     * Реплицируемый кэш
     */
    REPLICATED(true);

    private final boolean serializeValueFields;

    CacheType(final boolean serializeValueFields) {
        this.serializeValueFields = serializeValueFields;
    }

    /**
     * Возвращает признак, нужно ли сериализовывать поля с новым и старым значением элемента кэша.
     *
     * @return признак, нужно ли сериализовывать поля с новым и старым значением элемента кэша.
     */
    public boolean serializeValueFields() {
        return this.serializeValueFields;
    }
}
