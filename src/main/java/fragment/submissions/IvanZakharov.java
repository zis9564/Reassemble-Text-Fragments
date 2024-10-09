package fragment.submissions;

import java.util.*;
import java.util.regex.Pattern;
import java.util.function.Function;
import java.util.regex.MatchResult;
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

        private Fragment FULL;
        private Fragment LEFT;
        private Fragment RIGHT;
        private StringBuilder text;

        public String assemble(String line) {
            LinkedList<Fragment> fragments = Arrays.stream(line.split(";"))
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .map(Fragment::new)
                    .collect(Collectors.toCollection(LinkedList::new));

            if (fragments.isEmpty()) {
                return "";
            }
            text = new StringBuilder(fragments.pollFirst().payload);

            while (!fragments.isEmpty()) {
                if (FULL == null) {
                    FULL = OverlapCalculator.of(FULL, LEFT, RIGHT)
                            .calculateFullOverlap(fragments, text.toString());
                }
                if (LEFT == null) {
                    LEFT = OverlapCalculator.of(FULL, LEFT, RIGHT)
                            .calculateLeftOverlap(fragments, text.toString());
                }
                if (RIGHT == null) {
                    RIGHT = OverlapCalculator.of(FULL, LEFT, RIGHT)
                            .calculateRightOverlap(fragments, text.toString());
                }
                fragments.remove(merge(FULL, LEFT, RIGHT));
            }
            return text.toString();
        }

        private Fragment merge(Fragment full, Fragment left, Fragment right) {
            if (safe.apply(full).fullOverlap >= Math.max(safe.apply(left).leftOverlap, safe.apply(right).rightOverlap)) {
                return mergeFull(full);
            }
            if (safe.apply(left).leftOverlap > safe.apply(right).rightOverlap) {
                return mergeLeft(left);
            }
            else {
                return mergeRight(right);
            }
        }

        private Fragment mergeFull(Fragment full) {
            // no need to merge
            FULL = null;
            return full;
        }

        private Fragment mergeLeft(Fragment left) {
            text.insert(0, left.payload.substring(0, left.length - left.leftOverlap));
            LEFT = null;
            return left;
        }

        private Fragment mergeRight(Fragment right) {
            text.append(right.payload.substring(right.rightOverlap + 1));
            RIGHT = null;
            return right;
        }
    }

    //----------------------------------------------------------------------------------------------------------------------
    static class OverlapCalculator {

        private Fragment FULL;
        private Fragment LEFT;
        private Fragment RIGHT;

        public static OverlapCalculator of(Fragment full, Fragment left, Fragment right) {
            return new OverlapCalculator(full, left, right);
        }

        public Fragment calculateFullOverlap(List<Fragment> fragmentList, String text) {
            fragmentList.forEach(fragment -> {
                if (checkFullOverlap(text, fragment.payload)) {
                    fragment.fullOverlap = fragment.length;
                    setBigger(fragment, FULL);
                }
            });
            return FULL;
        }

        public Fragment calculateLeftOverlap(List<Fragment> fragmentList, String text) {
            fragmentList.forEach(fragment -> {
                if (safe.apply(LEFT).leftOverlap > fragment.length) {
                    // reducing useless calculations because our collection is sorted.
                    return;
                }
                fragment.leftOverlap = checkLeftOverlap(text, fragment.payload);
                setBigger(fragment, LEFT);
            });
            return LEFT;
        }

        public Fragment calculateRightOverlap(List<Fragment> fragmentList, String text) {
            fragmentList.forEach(fragment -> {
                if (safe.apply(RIGHT).rightOverlap > fragment.length) {
                    // reducing useless calculations because our collection is sorted.
                    return;
                }
                fragment.rightOverlap = checkRightOverlap(text, fragment.payload);
                setBigger(fragment, RIGHT);
            });
            return RIGHT;
        }

        private boolean checkFullOverlap(String text, String fragment) {
            return text.contains(fragment);
        }

        private int checkLeftOverlap(String text, String fragment) {
            String firstSymbol = String.valueOf(text.charAt(0));
            // find all 'anchors' by lastSymbol in text and then compare substrings until we get first match
            return Pattern.compile(Pattern.quote(firstSymbol))
                    .matcher(fragment)
                    .results()
                    .map(MatchResult::start)
                    .filter(i -> fragment.substring(i).equals(text.substring(0, (fragment.length() - i))))
                    .map(i -> fragment.length() - i) // calculate overlap
                    .findFirst()
                    .orElse(0);
        }

        private int checkRightOverlap(String result, String token) {
            String lastSymbol = String.valueOf(result.charAt(result.length()-1));
            // we find all 'anchors' by lastSymbol in result and then compare substrings until we get first match
            return Pattern.compile(Pattern.quote(lastSymbol))
                    .matcher(token)
                    .results()
                    .map(MatchResult::start)
                    .sorted(Collections.reverseOrder())
                    .filter(i -> token.substring(0, (i + 1)).equals(result.substring(result.length() - token.substring(0, (i + 1)).length())))
                    .findFirst()
                    .orElse(0);
        }

        private void setBigger(Fragment candidate, Fragment ongoing) {
            FULL = (candidate.fullOverlap > safe.apply(ongoing).fullOverlap) ? candidate : ongoing;
            LEFT = (candidate.leftOverlap > safe.apply(ongoing).leftOverlap) ? candidate : ongoing;
            RIGHT = (candidate.rightOverlap > safe.apply(ongoing).rightOverlap) ? candidate : ongoing;
        }

        private OverlapCalculator(Fragment full, Fragment left, Fragment right) {
            this.FULL = full;
            this.LEFT = left;
            this.RIGHT = right;
        }
    }

//----------------------------------------------------------------------------------------------------------------------

    static class Fragment {

        int length;
        int leftOverlap;
        int rightOverlap;
        int fullOverlap;
        String payload;

        public Fragment() {}

        public Fragment(String payload) {
            this.payload = payload;
            this.length = payload.length();
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
//----------------------------------------------------------------------------------------------------------------------

    // simple NPE protection to reduce code complexity
    static Function<Fragment, Fragment> safe = (f) -> (f == null) ? new Fragment() : f;
}