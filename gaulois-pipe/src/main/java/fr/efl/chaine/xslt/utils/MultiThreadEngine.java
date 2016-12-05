/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.utils;

import fr.efl.chaine.xslt.GauloisPipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * The multi-thread engine
 * @param <T> The type to process
 */
@Deprecated
public class MultiThreadEngine<T> {

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GauloisPipe.class);
    /**
     * The thread sleep duration.
     */
    private static final int SLEEP_DURATION = 1000;
    /**
     * The threads number.
     */
    private final int nbThreads;
    /**
     * The runner.
     */
    private final MultiThreadRunner<T> runner;
    /**
     * Le nom de l'instance à utiliser dans les logs
     */
    private final String instanceName;

    /**
     * The default constructor.
     *
     * @param nbThreads the threads number
     * @param runner The runner to use
     * @param instanceName LE nom de l'instance à utiliser dans les logs
     */
    public MultiThreadEngine(int nbThreads, MultiThreadRunner<T> runner, String instanceName) {
        this.nbThreads = nbThreads;
        this.runner = runner;
        this.instanceName = instanceName;
    }

    public void execute(List<T> inputs) {
        LOGGER.info("["+instanceName+"] Execute pipe on {} inputs", inputs.size());
        final Queue<T> inputsQueue = new ArrayBlockingQueue<>(inputs.size(), true, inputs);
        List<Thread> threads = new ArrayList<>(nbThreads);
        final boolean[] ends = new boolean[nbThreads];
        Arrays.fill(ends, false);
        for (int i = 0; i < nbThreads; i++) {
            final int index = i;
            threads.add(new Thread(new Runnable() {
                /**
                 * The run method.
                 */
                @Override
                public void run() {
                    try {
                        T input = inputsQueue.poll();
                        while (input != null) {
                            LOGGER.info("["+instanceName+"] Remaining inputs size is {} - {}", inputsQueue.size(), input);
                            runner.run(input);
                            input = inputsQueue.poll();
                        }
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    } finally {
                        ends[index] = true;
                    }
                }
            }));
        }
        for (Thread thread : threads) {
            thread.start();
        }
        while (notFinish(ends)) {
            try {
                Thread.sleep(SLEEP_DURATION);
            } catch (InterruptedException e) {
                LOGGER.warn("["+instanceName+"] "+e.getMessage(), e);
            }
        }
    }

    /**
     * Say if treatement was finsihed.
     *
     * @param ends the thread end values
     * @return true if the treatement was finished
     */
    private boolean notFinish(boolean[] ends) {
        for (boolean end : ends) {
            if (!end) {
                return true;
            }
        }
        return false;
    }
}
