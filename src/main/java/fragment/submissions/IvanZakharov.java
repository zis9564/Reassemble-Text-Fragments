package fragment.submissions;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;

public class IvanZakharov {

    private static final Assembler assembler = new Assembler();

    public static void main(String[] args) throws IOException {

        try (BufferedReader in = new BufferedReader(new FileReader(args[0]))) {
            in.lines()
                    .map(assembler::assemble)
                    .forEach(System.out::println);
        }
    }

    static class Assembler {

        private Fragment mergeCandidate;

        public String assemble(String line) {
            LinkedList<String> fragments = Arrays.stream(line.split(";"))
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .collect(Collectors.toCollection(LinkedList::new));

            while (fragments.size() > 1) {
                List<Fragment> collection = new ArrayList<>(fragments.stream().map(Fragment::new).toList());
                fragments.forEach(f -> {
                    TreeSet<Overlap> overlaps = new TreeSet<>(Comparator.comparingInt(Overlap::getIndex));
                    collection.remove(new Fragment(f)); // remove element (f) from the collection to avoid self comparison
                    overlaps.add(calculateOverlap(collection, f));
                    mergeCandidate = (safeO.apply(overlaps).getIndex() > safeF.apply(mergeCandidate).getOverlapIndex())
                            ? new Fragment(f, overlaps.last())
                            : mergeCandidate;
                });
                fragments.remove(safeF.apply(mergeCandidate).payload);
                fragments.remove(safeF.apply(mergeCandidate).getOverlap());
                fragments.addFirst(safeF.apply(mergeCandidate).merge());
                mergeCandidate = null;
            }
            return fragments.getFirst();
        }

        private Overlap calculateOverlap(List<Fragment> collection, String fragment) {
            if (collection.isEmpty()) {
                return new Overlap();
            }
            TreeSet<Overlap> intersections = new TreeSet<>(Comparator.comparingInt(Overlap::getIndex));
            intersections.add(new OverlapCalculator().calculateFullOverlap(collection, fragment));
            intersections.add(new OverlapCalculator().calculateLeftOverlap(collection, fragment));
            intersections.add(new OverlapCalculator().calculateRightOverlap(collection, fragment));
            return intersections.last();
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    static class OverlapCalculator {

        public OverlapCalculator () {}

        /**
         * Returns biggest FULL overlap from collection "fragmentList" relative to argument 'text'
         * @param fragmentList list of fragments where from the biggest intersection should be found
         * @param text word for comparisons
         * @return Overlap object containing information about intersection
         */
        public Overlap calculateFullOverlap(List<Fragment> fragmentList, String text) {
            TreeSet<Overlap> overlaps = new TreeSet<>(Comparator.comparingInt(Overlap::getIndex));
            fragmentList.forEach(fragment -> {
                if (!text.contains(fragment.payload)) {
                    return;
                }
                overlaps.add(new Overlap(fragment.payload.length(), Side.FULL, fragment.payload));
            });
            return safeO.apply(overlaps);
        }

        /**
         * Returns biggest LEFT overlap from collection "fragmentList" relative to argument 'text'
         * @param fragmentList list of fragments where from the biggest intersection should be found
         * @param text word for comparisons
         * @return Overlap object containing information about intersection
         */
        public Overlap calculateLeftOverlap(List<Fragment> fragmentList, String text) {
            TreeSet<Overlap> overlaps = new TreeSet<>(Comparator.comparingInt(Overlap::getIndex));
            fragmentList.forEach(fragment -> {
                if (text.contains(fragment.payload)) {
                    return;
                }
                overlaps.add(new Overlap(checkLeftOverlap(text, fragment.payload), Side.LEFT, fragment.payload));
            });
            return safeO.apply(overlaps);
        }

        /**
         * Returns biggest RIGHT overlap from collection "fragmentList" relative to argument 'text'
         * @param fragmentList list of fragments where from the biggest intersection should be found
         * @param text word for comparisons
         * @return Overlap object containing information about intersection
         */
        public Overlap calculateRightOverlap(List<Fragment> fragmentList, String text) {
            TreeSet<Overlap> overlaps = new TreeSet<>(Comparator.comparingInt(Overlap::getIndex));
            fragmentList.forEach(fragment -> {
                if (text.contains(fragment.payload)) {
                    return;
                }
                overlaps.add(new Overlap(checkRightOverlap(text, fragment.payload), Side.RIGHT, fragment.payload));
            });
            return safeO.apply(overlaps);
        }

        /**
         * Compares word "text" with element of collection "fragment" and find the longest LEFT intersection of two words
         * @param text word for comparisons
         * @param fragment element of collection
         * @return overlap index
         */
        private static int checkLeftOverlap(String text, String fragment) {
            int overlapIndex = text.length() - 1;
            while (!fragment.regionMatches(false, fragment.length() - overlapIndex, text, 0, overlapIndex)) {
                overlapIndex--;
            }
            return overlapIndex;
        }

        /**
         * Compares word "text" with element of collection "fragment" and find the longest RIGHT intersection of two words
         * @param text word for comparisons
         * @param fragment element of collection
         * @return overlap index
         */
        private static int checkRightOverlap(String text, String fragment) {
            int overlapIndex = fragment.length() - 1;
            while (!text.regionMatches(false, text.length() - overlapIndex, fragment, 0, overlapIndex)) {
                overlapIndex--;
            }
            return overlapIndex;
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

        public Fragment(String payload, Overlap overlap) {
            this.intersections = new TreeSet<>(Comparator.comparingInt(Overlap::getIndex));
            this.stringBuilder = new StringBuilder(payload);
            intersections.add(overlap);
            this.payload = payload;
        }

        public Fragment(String payload) {
            this.intersections = new TreeSet<>(Comparator.comparingInt(Overlap::getIndex));
            this.stringBuilder = new StringBuilder(payload);
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
                Overlap mergeCandidate = this.intersections.last();
                switch (mergeCandidate.side) {
                    case FULL -> {
                        return this.payload;
                    }
                    case LEFT -> {
                        return mergeLeft(mergeCandidate);
                    }
                    case RIGHT -> {
                        return mergeRight(mergeCandidate);
                    }
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

    static class Overlap {

        final int index;
        final Side side;
        final String payload;

        public Overlap() {
            this.index = -1;
            side = null;
            payload = "";
        }

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

    // simple NPE protection to reduce code complexity
    static Function<Fragment, Fragment> safeF = (f) -> (f == null) ? new Fragment() : f;

    // simple NPE protection to reduce code complexity
    static Function<TreeSet<Overlap>, Overlap> safeO = (f) -> (f == null || f.isEmpty()) ? new Overlap() : f.last();

}