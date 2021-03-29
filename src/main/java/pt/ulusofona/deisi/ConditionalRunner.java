/*-
 * ========================LICENSE_START=================================
 * "Run if" extension for JUnit
 * %%
 * Copyright (C) 2021 Pedro Alves
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package pt.ulusofona.deisi;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerScheduler;
import org.junit.runners.model.Statement;

import java.util.*;

/**
 * A runner which is aware of the {@link RunIf} of its {@link org.junit.Test}s
 */
public class ConditionalRunner extends BlockJUnit4ClassRunner {

    private Sorter fSorter = Sorter.NULL;

    private List<FrameworkMethod> fFilteredChildren;

    private RunnerScheduler fScheduler = new RunnerScheduler() {
        public void schedule(Runnable childStatement) {
            childStatement.run();
        }

        public void finished() {
            // do nothing
        }
    };

    /**
     * Creates a ConditionalRunner to run {@code klass}
     *
     * @param klass the class affected by Conditional Runner
     * @throws org.junit.runners.model.InitializationError
     *          if the test class is malformed.
     */
    public ConditionalRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    public void sort(Sorter sorter) {
        fSorter = sorter;
        for (FrameworkMethod each : getFilteredChildren()) {
            sortChild(each);
        }
        Collections.sort(getFilteredChildren(), comparator());
        assumptionSort(getFilteredChildren());
    }

    private void sortChild(FrameworkMethod child) {
        fSorter.apply(child);
    }

    private Comparator<? super FrameworkMethod> comparator() {
        return (Comparator<FrameworkMethod>) (o1, o2) ->
                fSorter.compare(describeChild(o1), describeChild(o2));
    }

    /**
     * Returns a {@link Statement}: Call {@link #runChild(Object, RunNotifier)}
     * on each object returned by {@link #getChildren()} (subject to any imposed
     * filter and sort)
     */
    protected Statement childrenInvoker(final RunNotifier notifier) {
        return new Statement() {
            @Override
            public void evaluate() {
                runChildren(notifier, Collections.synchronizedSet(new HashSet<String>()));
            }
        };
    }

    private void runChildren(final RunNotifier notifier, final Set<String> invalidAssumptions) {
        RunListener l = new RunListener() {
            @Override
            public void testFailure(Failure failure) {
                invalidAssumptions.add(failure.getDescription().getMethodName());
            }

            @Override
            public void testAssumptionFailure(Failure failure) {
                invalidAssumptions.add(failure.getDescription().getMethodName());
            }

            @Override
            public void testIgnored(Description description)  {
                invalidAssumptions.add(description.getMethodName());
            }
        };
        notifier.addListener(l);
        for (final FrameworkMethod each : getFilteredChildren()) {
            fScheduler.schedule(new Runnable() {
                public void run() {
                    ConditionalRunner.this.runChild(each, notifier, invalidAssumptions);
                }
            });
        }
        fScheduler.finished();
    }

    protected void runChild(final FrameworkMethod method, RunNotifier notifier, Set<String> invalidAssumptions) {
        RunIf annotation = method.getAnnotation(RunIf.class);
        boolean invalidAnnotation = false;
        if (annotation != null) {
            for (String assumption : annotation.value()) {
                if (invalidAssumptions.contains(assumption)) {
                    invalidAnnotation = true;
                    break;
                }
            }
        }
        if (invalidAnnotation) {
            invalidAssumptions.add(method.getName());
            notifier.fireTestIgnored(describeChild(method));
        } else {
            runChild(method, notifier);
        }
    }

    @Override
    protected final void runChild(FrameworkMethod method, RunNotifier notifier) {
        super.runChild(method, notifier);
    }

    private void assumptionSort(List<FrameworkMethod> methods) {
        int size = methods.size();
        for (int i = 0; i < size; i++) {
            FrameworkMethod m = methods.get(i);
            RunIf assumes = m.getAnnotation(RunIf.class);
            if (assumes != null) {
                Set<String> assumptions = new HashSet<String>(Arrays.asList(assumes.value()));
                for (int j = size - 1; j > i; j--) {
                    if (assumptions.contains(methods.get(j).getName())) {
                        methods.add(j, methods.remove(i));
                        break;
                    }
                }
            }
        }
    }

    private List<FrameworkMethod> getFilteredChildren() {
        if (fFilteredChildren == null) {
            fFilteredChildren = new ArrayList<>(getChildren());
            assumptionSort(fFilteredChildren);
        }
        return fFilteredChildren;
    }


    @Override
    public Description getDescription() {
        Description description = Description.createSuiteDescription(getName(),
                getTestClass().getAnnotations());
        for (FrameworkMethod child : getFilteredChildren()) {
            description.addChild(describeChild(child));
        }
        return description;
    }

    public void filter(Filter filter) throws NoTestsRemainException {
        for (Iterator<FrameworkMethod> iter = getFilteredChildren().iterator(); iter.hasNext(); ) {
            FrameworkMethod each = iter.next();
            if (shouldRun(filter, each)) {
                try {
                    filter.apply(each);
                } catch (NoTestsRemainException e) {
                    iter.remove();
                }
            } else {
                iter.remove();
            }
        }
        if (getFilteredChildren().isEmpty()) {
            throw new NoTestsRemainException();
        }
    }

    private boolean shouldRun(Filter filter, FrameworkMethod each) {
        return filter.shouldRun(describeChild(each));
    }

    /**
     * Sets a scheduler that determines the order and parallelization
     * of children.  Highly experimental feature that may change.
     */
    public void setScheduler(RunnerScheduler scheduler) {
        this.fScheduler = scheduler;
    }

}
