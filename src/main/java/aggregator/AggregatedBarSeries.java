package aggregator;

import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class AggregatedBarSeries extends BaseBarSeries {
    private final Duration bigBarTimePeriod;
    private Bar currentSmallBar;
    private Bar previousBigBarState;
    private final List<LocalTime> endTimesGrid = new ArrayList<>();

    public AggregatedBarSeries(Duration bigBarTimePeriod) {
        validateTimePeriod(bigBarTimePeriod);
        fillEndTimesGrid(bigBarTimePeriod);
        this.bigBarTimePeriod = bigBarTimePeriod;
    }

    @Override
    public void addBar(Bar smallBar, boolean replace) {
        if (replace) {
            replaceCurrentSmallBar(smallBar);
        } else {
            processAddBar(smallBar);
        }
    }

    private void processAddBar(Bar smallBar) {
        validateSmallBar(smallBar);

        // If smallBar is first in all BarSeries or if smallBar begins after the end of current bigBar - then create new bigBar
        if (shouldCreateNewBigBar(smallBar.getEndTime().withZoneSameInstant(ZoneOffset.UTC))) {
            Bar newBigBar = createBigBar(smallBar);
            previousBigBarState = this.getBarData().isEmpty() ? null : this.getLastBar();
            super.addBar(newBigBar, false);
        } else {
            updateCurrentBigBar(smallBar);
        }
        currentSmallBar = smallBar;
    }

    private boolean shouldCreateNewBigBar(ZonedDateTime smallBarEndTimeUTC) {
        return this.getBeginIndex() == -1 ||
                this.getLastBar().getEndTime().isBefore(smallBarEndTimeUTC);
    }

    private void replaceCurrentSmallBar(Bar newSmallBar) {
        Objects.requireNonNull(newSmallBar);
        if (newSmallBar.getBeginTime().isBefore(currentSmallBar.getBeginTime())) {
            throw new IllegalArgumentException("Cannot replace bar. Begin time < current small bar begin time");
        }
        super.addBar(previousBigBarState, true);
        addBar(newSmallBar, false);
    }

    private void updateCurrentBigBar(Bar smallBar) {
        Bar currentBigBar = this.getLastBar();
        Bar updatedCurrentBigBar = BaseBar.builder()
                .timePeriod(currentBigBar.getTimePeriod())
                .endTime(currentBigBar.getEndTime())
                .openPrice(currentBigBar.getOpenPrice())
                .highPrice(currentBigBar.getHighPrice().max(smallBar.getHighPrice()))
                .lowPrice(currentBigBar.getLowPrice().min(smallBar.getLowPrice()))
                .closePrice(smallBar.getClosePrice())
                .volume(smallBar.getVolume().plus(currentBigBar.getVolume()))
                .amount(smallBar.getAmount().plus(currentBigBar.getAmount()))
                .build();
        previousBigBarState = currentBigBar;
        super.addBar(updatedCurrentBigBar, true);
    }

    private Bar createBigBar(Bar smallBar) {
        ZonedDateTime smallBarBeginTimeUTC = smallBar.getBeginTime().withZoneSameInstant(ZoneOffset.UTC);
        int currentBigBarEndTimeIndex = findBigBarEndTimeIndex(smallBarBeginTimeUTC.toLocalTime());

        ZonedDateTime midnightUTC = smallBarBeginTimeUTC.with(LocalTime.MIN);
        ZonedDateTime bigBarEndDateTimeUTC = midnightUTC.with(endTimesGrid.get(currentBigBarEndTimeIndex));

        Duration bigBarTimePeriod = cutBigBarTimePeriodIfExceedMidnight(currentBigBarEndTimeIndex);
        return new BaseBar(bigBarTimePeriod, bigBarEndDateTimeUTC,
                smallBar.getOpenPrice(), smallBar.getHighPrice(), smallBar.getLowPrice(), smallBar.getClosePrice(),
                smallBar.getVolume(), smallBar.getAmount());
    }

    private Duration cutBigBarTimePeriodIfExceedMidnight(int currentBigBarEndTimeIndex) {
        return (currentBigBarEndTimeIndex == endTimesGrid.size() - 1) && (endTimesGrid.size() > 1)
                ? Duration.between(
                endTimesGrid.get(currentBigBarEndTimeIndex - 1),
                LocalTime.of(23, 59, 59, 999999999)).plusNanos(1)
                : this.bigBarTimePeriod;
    }

    private void validateSmallBar(Bar smallBar) {
        Objects.requireNonNull(smallBar);
        if ((currentSmallBar != null) &&
                (smallBar.getBeginTime().withZoneSameInstant(ZoneOffset.UTC)
                        .isBefore(currentSmallBar.getEndTime().withZoneSameInstant(ZoneOffset.UTC)))) {
            throw new IllegalArgumentException(
                    "Cannot add a bar with begin time < current small bar end time");
        }
    }

    private int findBigBarEndTimeIndex(LocalTime smallBarBeginTime) {
        return IntStream.range(0, endTimesGrid.size()).filter(i -> {
            LocalTime bigBarEndTime = endTimesGrid.get(i);
            return (smallBarBeginTime.isBefore(bigBarEndTime));
        }).findFirst().orElse(endTimesGrid.size() - 1);

    }

    private void validateTimePeriod(Duration bigBarTimePeriod) {
        Objects.requireNonNull(bigBarTimePeriod);
        if (bigBarTimePeriod.toHours() > 24) {
            throw new IllegalArgumentException("Interval can not be > 24 hours");
        }
        if (bigBarTimePeriod.toMillis() < 1000) {
            throw new IllegalArgumentException("Interval can not be < 1 second");
        }
    }

    private void fillEndTimesGrid(Duration interval) {
        LocalTime startOfBigBar = LocalTime.MIN;
        LocalTime endOfDay = LocalTime.MAX;

        while (startOfBigBar.isBefore(endOfDay)) {
            LocalTime endOfBigBar = startOfBigBar.plus(interval);

            // To prevent endless loop for 00:00 when midnight
            if (endOfBigBar.equals(LocalTime.MIN) && !endTimesGrid.isEmpty()) {
                endTimesGrid.add(LocalTime.MIN);
                break;
            }
            // To cut last endOfBigBar to 00.00 when 24hours%bigBarTimePeriod != 0
            if (isBigBarEndCrossMidnight(endOfBigBar)) {
                endTimesGrid.add(LocalTime.MIN);
                break;
            }
            endTimesGrid.add(endOfBigBar);
            startOfBigBar = endOfBigBar;
        }
    }


    private boolean isBigBarEndCrossMidnight(LocalTime endOfBigBar) {
        if (endTimesGrid.isEmpty()) {
            return false;
        }
        LocalTime previousBigBarEndTime = endTimesGrid.get(endTimesGrid.size() - 1);
        return endOfBigBar.isBefore(previousBigBarEndTime);
    }


}
