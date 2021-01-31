package edu.coursera.parallel;

import java.util.concurrent.Phaser;

/**
 * Wrapper class for implementing one-dimensional iterative averaging using
 * phasers.
 */
public final class OneDimAveragingPhaser {
    /**
     * Default constructor.
     */
    private OneDimAveragingPhaser() {
    }

    /**
     * Sequential implementation of one-dimensional iterative averaging.
     *
     * @param iterations The number of iterations to run
     * @param myNew      A double array that starts as the output array
     * @param myVal      A double array that contains the initial input to the
     *                   iterative averaging problem
     * @param n          The size of this problem
     */
    public static void runSequential(final int iterations, final double[] myNew,
                                     final double[] myVal, final int n) {
        double[] next = myNew;
        double[] curr = myVal;

        for (int iter = 0; iter < iterations; iter++) {
            for (int j = 1; j <= n; j++) {
                next[j] = (curr[j - 1] + curr[j + 1]) / 2.0;
            }
            double[] tmp = curr;
            curr = next;
            next = tmp;
        }
    }

    /**
     * An example parallel implementation of one-dimensional iterative averaging
     * that uses phasers as a simple barrier (arriveAndAwaitAdvance).
     *
     * @param iterations The number of iterations to run
     * @param myNew      A double array that starts as the output array
     * @param myVal      A double array that contains the initial input to the
     *                   iterative averaging problem
     * @param n          The size of this problem
     * @param tasks      The number of threads/tasks to use to compute the solution
     */
    public static void runParallelBarrier(final int iterations,
                                          final double[] myNew, final double[] myVal, final int n,
                                          final int tasks) {
        Phaser ph = new Phaser(0); //"0" means not registering self!!!
//        Registration of tasks on phaser is for awaitAdvance() method to count the number of threads/tasks that should signal before advancing
        ph.bulkRegister(tasks);

        //creation of tasks/threads
        Thread[] threads = new Thread[tasks];

        for (int ii = 0; ii < tasks; ii++) {
            final int i = ii;

            threads[ii] = new Thread(() -> {
                double[] threadPrivateMyVal = myVal;
                double[] threadPrivateMyNew = myNew;

                for (int iter = 0; iter < iterations; iter++) {
                    final int left = i * (n / tasks) + 1;
                    final int right = (i + 1) * (n / tasks);

                    for (int j = left; j <= right; j++) {
                        threadPrivateMyNew[j] = (threadPrivateMyVal[j - 1]
                                + threadPrivateMyVal[j + 1]) / 2.0;
                    }
                    ph.arriveAndAwaitAdvance();

                    double[] temp = threadPrivateMyNew;
                    threadPrivateMyNew = threadPrivateMyVal;
                    threadPrivateMyVal = temp;
                }
            });
            threads[ii].start();
        }

        for (int ii = 0; ii < tasks; ii++) {
            try {
                threads[ii].join();  //It will block until thread[ii] ends!!!
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * A parallel implementation of one-dimensional iterative averaging that
     * uses the Phaser.arrive and Phaser.awaitAdvance APIs to overlap
     * computation with barrier completion.
     * <p>
     * TODO Complete this method based on the provided runSequential and
     * runParallelBarrier methods.
     *
     * @param iterations The number of iterations to run
     * @param myNew      A double array that starts as the output array
     * @param myVal      A double array that contains the initial input to the
     *                   iterative averaging problem
     * @param n          The size of this problem
     * @param tasks      The number of threads/tasks to use to compute the solution
     */
    public static void runParallelFuzzyBarrier(final int iterations,
                                               final double[] myNew, final double[] myVal, final int n,
                                               final int tasks) {
        Phaser[] phaserArray = new Phaser[tasks];
        for (int i = 0; i < phaserArray.length; i++) {
            phaserArray[i] = new Phaser(1);
        }

        //creation of tasks/threads
        Thread[] threads = new Thread[tasks];

        for (int ii = 0; ii < tasks; ii++) {
            final int i = ii;

            threads[ii] = new Thread(() -> {
                double[] threadPrivateMyVal = myVal;
                double[] threadPrivateMyNew = myNew;

                for (int iter = 0; iter < iterations; iter++) {
                    final int left = i * (n / tasks) + 1;
                    final int right = (i + 1) * (n / tasks);

                    for (int j = left; j <= right; j++) {
                        threadPrivateMyNew[j] = (threadPrivateMyVal[j - 1]
                                + threadPrivateMyVal[j + 1]) / 2.0;
                    }

//                    System.out.printf("Arrived task %d; Phase/Iter # %d%n", i, iter);
                    int currPhase = phaserArray[i].arrive();// it returns 0-indexed phase #!!!

                    if (i - 1 >= 0) {
//                        System.out.printf(
//                                "Arrived task %d waiting for task %d in the phase %d to advance to the next phase%n",
//                                i, i - 1, iter);
                        phaserArray[i - 1].awaitAdvance(currPhase);  // in this case, phase # is iteration #!!!!
                    }
                    if (i + 1 < tasks) {
//                        System.out.printf(
//                                "Arrived task %d waiting for task %d in the phase %d to advance to the next phase%n",
//                                i, i + 1, iter);
                        phaserArray[i + 1].awaitAdvance(currPhase);
                    }

                    //SWAP!!!!!Cleanup work / prep work for the next iteration!!!
                    double[] temp = threadPrivateMyNew;
                    threadPrivateMyNew = threadPrivateMyVal;
                    threadPrivateMyVal = temp;
                }
            });
            threads[ii].start();
        }

        for (int ii = 0; ii < tasks; ii++) {
            try {
                threads[ii].join(); //It will block until thread[ii] ends!!!
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
