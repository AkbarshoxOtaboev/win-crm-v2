package uz.script.wincrm.filial;

import java.util.function.Supplier;

public final class FilialContext {

    private static final ThreadLocal<State> HOLDER = new ThreadLocal<>();

    private FilialContext() {
    }

    public static void bind(Long filialId, boolean superAdmin) {
        HOLDER.set(new State(true, filialId, superAdmin));
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static boolean isBound() {
        State state = HOLDER.get();
        return state != null && state.bound;
    }

    public static Long getFilialId() {
        State state = HOLDER.get();
        return state == null ? null : state.filialId;
    }

    public static boolean isSuperAdmin() {
        State state = HOLDER.get();
        return state != null && state.superAdmin;
    }

    /**
     * Runs {@code action} with the filial filter switched to {@code filialId}
     * ({@code null} = all filials). Entities lazily loaded inside must be read inside too.
     */
    public static <T> T callAs(Long filialId, Supplier<T> action) {
        State previous = HOLDER.get();
        HOLDER.set(new State(true, filialId, previous != null && previous.superAdmin));
        try {
            return action.get();
        } finally {
            if (previous == null) {
                HOLDER.remove();
            } else {
                HOLDER.set(previous);
            }
        }
    }

    public static void runAs(Long filialId, Runnable action) {
        callAs(filialId, () -> {
            action.run();
            return null;
        });
    }

    private record State(boolean bound, Long filialId, boolean superAdmin) {
    }
}
