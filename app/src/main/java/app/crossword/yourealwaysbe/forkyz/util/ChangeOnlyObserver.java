
package app.crossword.yourealwaysbe.forkyz.util;

import androidx.lifecycle.Observer;

/**
 * An observer that does not get the initial value, only changes
 */
public abstract class ChangeOnlyObserver<T> implements Observer<T> {

    private boolean hadFirst = false;

    @Override
    public void onChanged(T value) {
        if (hadFirst)
            onTrueChange(value);
        else
            hadFirst = true;
    }

    public abstract void onTrueChange(T value);
}

