package uz.script.wincrm.filial;

import java.util.function.Supplier;

/**
 * Resolves the {@code filialFilter} parameter per query. {@code 0} disables filtering
 * (unbound threads, super admin without a selected filial).
 */
public class CurrentFilialIdResolver implements Supplier<Long> {

    public static final long ALL_FILIALS = 0L;

    @Override
    public Long get() {
        if (!FilialContext.isBound()) {
            return ALL_FILIALS;
        }
        Long filialId = FilialContext.getFilialId();
        return filialId == null ? ALL_FILIALS : filialId;
    }
}
