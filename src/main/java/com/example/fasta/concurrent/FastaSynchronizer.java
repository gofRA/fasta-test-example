package com.example.fasta.concurrent;

import java.util.ArrayList;
import java.util.List;

/**
 * Synchronizes threads in the order of file names specified as program arguments.
 *
 * @author rdanilov
 * @since 21.10.2018
 */
public class FastaSynchronizer {

    private List<FastaFileWorker> tasks;
    private int currentTaskNum;
    private int currentRowNum;
    private boolean rowChanged;

    /**
     * Constructs an instance of {@link FastaSynchronizer} with initial values
     */
    public FastaSynchronizer() {
        this.tasks = new ArrayList<>();
        this.rowChanged = false;
        this.currentTaskNum = 0;
        this.currentRowNum = 1;
    }

    /**
     * Adds task to tasks list to be run in particular order
     *
     * @param taskToAdd task to be added in task list
     */
    void add(FastaFileWorker taskToAdd) {
        tasks.add(taskToAdd);
    }

    /**
     * Checks if task is current task that should work now
     *
     * @param task thread to be checked
     * @return result of the check
     */
    boolean isMyTurn(FastaFileWorker task) {
        return tasks.get(currentTaskNum).equals(task);
    }


    /**
     * Moves the ability to work to the next task.
     * If all tasks were performed at this stage, registers row change event
     * and current row number is incremented
     */
    void moveOrder() {
        if (!tasks.isEmpty()) {
            int nextTaskNum = currentTaskNum + 1;
            rowChanged = nextTaskNum == tasks.size();
            if (rowChanged) {
                currentRowNum++;
            }
            currentTaskNum = nextTaskNum % tasks.size();
        }
    }

    /**
     * Remove task from tasks list. It means that the task has completed its execution.
     * After the task is removed, all elements after the task are shifting left and we must return back
     * to previous task if only current task isn't the first task in the tasks list.
     *
     * @param taskToRemove task to be removed from tasks list
     */
    void remove(FastaFileWorker taskToRemove) {
        tasks.remove(taskToRemove);
        if (currentTaskNum != 0) {
            currentTaskNum--;
        }
    }

    /**
     * Checks if row has been changed since the last {{@link #moveOrder()}}.
     * After check the boolean flag is set to false, so the only one thread
     * can write a new description line
     *
     * @return result of the check
     */
    boolean isRowChanged() {
        if (rowChanged) {
            rowChanged = false;
            return true;
        }
        return false;
    }

    /**
     * @return current row number to be write into file as description row
     */
    int getCurrentRowNum() {
        return currentRowNum;
    }
}
