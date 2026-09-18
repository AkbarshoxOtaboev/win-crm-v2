package uz.script.wincrm.filial;

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

    private record State(boolean bound, Long filialId, boolean superAdmin) {
    }
}
