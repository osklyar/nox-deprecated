/**
 * Created by skol on 28.02.17.
 */
package nox.internal.gradlize;

public enum Duplicates {
    Overwrite(true),
    Forbid(false);

    private final boolean value;

    Duplicates(boolean value) {
        this.value = value;
    }

    public boolean permitted() {
        return value;
    }
}
