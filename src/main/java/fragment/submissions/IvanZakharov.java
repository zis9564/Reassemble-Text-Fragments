package fragment.submissions;

import java.util.*;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
This is third version of an algorithm.

The first version was implementation of direct approach.
The second version fixed an issue where merge-candidate was considering as biggest relative to the ‘anchor’ element without global comparison.
The third version made code simpler, more reliable and easy to read.
*/
public class IvanZakharov {

    private static final Assembler assembler = new Assembler();

    public static void main(String[] args) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(args[0]))) {
            in.lines().map(line ->
                            Arrays.stream(line.split(";"))
                                    .sorted(Comparator.comparingInt(String::length).reversed())
                                    .collect(Collectors.toList()))
                    .map(fragments -> assembler.assemble(new LinkedList<>(fragments)))
                    .forEach(System.out::println);
        }
    }

    static class Assembler {

        public String assemble(final LinkedList<String> fragments) {
            while (fragments.size() > 1) {
                TreeSet<Fragment> intersections = new TreeSet<>(Comparator.comparingInt(Fragment::getOverlapIndex));
                ArrayList<Fragment> collection = fragments.stream().map(Fragment::new).collect(Collectors.toCollection(ArrayList::new));

                fragments.forEach(f -> {
                        collection.remove(new Fragment(f)); // avoid self comparison
                        calculateOverlap(collection, intersections, f);
                });
                Optional.ofNullable(intersections.last())
                        .ifPresent(candidate -> {
                            fragments.remove(safeF.apply(candidate).payload);
                            fragments.remove(safeF.apply(candidate).getOverlap());
                            fragments.addFirst(candidate.merge());
                });
            }
            return fragments.getFirst();
        }

        private void calculateOverlap(List<Fragment> collection, TreeSet<Fragment> intersections, String fragment) {
            if (collection.isEmpty()) {
                return;
            }
            new OverlapCalculator().calcFull(collection, intersections, fragment);
            new OverlapCalculator().calcLeft(collection, intersections, fragment);
            new OverlapCalculator().calcRight(collection, intersections, fragment);
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    static class OverlapCalculator {

        public OverlapCalculator () {}

        /**
         * Returns biggest FULL overlap from collection "fragments" relative to argument 'anchor'
         * @param fragments list of fragments where from the biggest intersection should be found
         * @param anchor word for comparisons
         */
        public void calcFull(List<Fragment> fragments, TreeSet<Fragment> intersections, String anchor) {
            fragments.forEach(fragment -> {
                if (!anchor.contains(fragment.payload)) {
                    return;
                }
                intersections.add(new Fragment(anchor, new Overlap(fragment.payload.length(), Side.FULL, fragment.payload)));
            });
        }

        /**
         * Returns biggest LEFT overlap from collection "fragments" relative to argument 'anchor'
         * @param fragments list of fragments where from the biggest intersection should be found
         * @param anchor word for comparisons
         */
        public void calcLeft(List<Fragment> fragments, TreeSet<Fragment> intersections, String anchor) {
            fragments.forEach(fragment -> {
                if (anchor.contains(fragment.payload)) {
                    return;
                }
                intersections.add(new Fragment(anchor, new Overlap(checkLeft(anchor, fragment.payload), Side.LEFT, fragment.payload)));
            });
        }

        /**
         * Returns biggest RIGHT overlap from collection "fragments" relative to argument 'anchor'
         * @param fragments list of fragments where from the biggest intersection should be found
         * @param anchor word for comparisons
         */
        public void calcRight(List<Fragment> fragments, TreeSet<Fragment> intersections, String anchor) {
            fragments.forEach(fragment -> {
                if (anchor.contains(fragment.payload)) {
                    return;
                }
                intersections.add(new Fragment(anchor, new Overlap(checkRight(anchor, fragment.payload), Side.RIGHT, fragment.payload)));
            });
        }

        /**
         * Compares word "anchor" with element of collection "fragments" then find the longest LEFT intersection of two words
         * @param anchor word for comparisons
         * @param fragment element of collection
         */
        private static int checkLeft(String anchor, String fragment) {
            int index = anchor.length() - 1;
            while (!fragment.regionMatches(false, fragment.length() - index, anchor, 0, index)) {
                index--;
            }
            return index;
        }

        /**
         * Compares word "anchor" with element of collection "fragments" then find the longest RIGHT intersection of two words
         * @param anchor word for comparisons
         * @param fragment element of collection
         */
        private static int checkRight(String anchor, String fragment) {
            int index = fragment.length() - 1;
            while (!anchor.regionMatches(false, anchor.length() - index, fragment, 0, index)) {
                index--;
            }
            return index;
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    static class Fragment {

        final String payload;
        private StringBuilder stringBuilder;
        private final TreeSet<Overlap> intersections;

        public Fragment() {
            this.payload = "";
            this.intersections = new TreeSet<>(Comparator.comparingInt(Overlap::getIndex));
        }

        public Fragment(String payload) {
            this.intersections = new TreeSet<>(Comparator.comparingInt(Overlap::getIndex));
            this.stringBuilder = new StringBuilder(payload);
            this.payload = payload;
        }

        public Fragment(String payload, Overlap overlap) {
            this.intersections = new TreeSet<>(Comparator.comparingInt(Overlap::getIndex));
            this.stringBuilder = new StringBuilder(payload);
            intersections.add(overlap);
            this.payload = payload;
        }

        public int getOverlapIndex() {
            if (!this.intersections.isEmpty()) {
                return this.intersections.last().index;
            }
            return -1;
        }

        public String getOverlap() {
            if (!this.intersections.isEmpty()) {
                return this.intersections.last().payload;
            }
            return "";
        }

        public String merge() {
            if (!this.intersections.isEmpty()) {
                Overlap candidate = this.intersections.last();
                switch (candidate.side) {
                    case FULL:
                        return this.payload;
                    case LEFT:
                        return mergeLeft(candidate);
                    case RIGHT:
                        return mergeRight(candidate);
                }
            }
            return this.payload;
        }

        private String mergeLeft(Overlap overlap) {
            return stringBuilder.insert(0, overlap.payload.substring(0, overlap.payload.length() - overlap.index)).toString();
        }

        private String mergeRight(Overlap overlap) {
            return stringBuilder.append(overlap.payload.substring(overlap.index)).toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Fragment fragment = (Fragment) o;
            return Objects.equals(payload, fragment.payload);
        }

        @Override
        public int hashCode() {
            return Objects.hash(payload);
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    static class Overlap {

        final int index;
        final Side side;
        final String payload;

        public Overlap(int index, Side side, String payload) {
            this.index = index;
            this.side = side;
            this.payload = payload;
        }

        public int getIndex() {
            return this.index;
        }
    }

    enum Side { LEFT, RIGHT, FULL }

    //------------------------------------------------------------------------------------------------------------------

    // simple NPE protection
    static Function<Fragment, Fragment> safeF = (f) -> (f == null) ? new Fragment() : f;

}