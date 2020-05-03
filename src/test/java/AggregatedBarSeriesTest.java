import aggregator.AggregatedBarSeries;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.ta4j.core.Bar;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import static util.TestDataFactory.*;


public class AggregatedBarSeriesTest {
    private AggregatedBarSeries aggregator;

    @Test
    public void shouldNotCreationFailWithValidTimePeriod() {
        aggregator = new AggregatedBarSeries(Duration.ofSeconds(55));
        aggregator = new AggregatedBarSeries(Duration.ofHours(1));
    }

    @Test
    public void shouldCreateBigBar() {
        aggregator = new AggregatedBarSeries(Duration.ofHours(6));
        Bar firstSmallBar = smallBarWithTimePeriodTwo(aggregator, "2020-02-05T07:12:59.999999999", 1);

        aggregator.addBar(firstSmallBar);
        Bar resultBigBar = aggregator.getLastBar();
        Assert.assertNotNull(resultBigBar);
    }

    @Test
    public void shouldCutLastBigBarTimePeriodIfDayNotDividedIntoEqualParts() {
        aggregator = new AggregatedBarSeries(Duration.ofHours(15));
        Bar smallBar = smallBarWithTimePeriodTwo(aggregator, "2020-02-05T21:12:59.999999999", 1);
        aggregator.addBar(smallBar);

        Duration expectedTimePeriod = Duration.between(
                LocalTime.of(15, 0),
                LocalTime.of(23, 59, 59, 999999999)
        ).plusNanos(1);

        Duration result = aggregator.getLastBar().getTimePeriod();
        Assert.assertEquals(expectedTimePeriod, result);
    }

    @Test
    public void shouldAggregateSmallBarsIntoBigBars() {
        aggregator = new AggregatedBarSeries(Duration.ofHours(6));
        Bar firstSmallBar = smallBarWithTimePeriodTwo(aggregator, "2020-02-05T07:12:59.999999999", 1);
        Bar secondSmallBar = smallBarWithTimePeriodTwo(aggregator, "2020-02-05T11:00:00.000000000", 1);
        Bar thirdSmallBar = smallBarWithTimePeriodTwo(aggregator, "2020-02-05T13:00:00.000000000", 2);
        Bar forthSmallBar = smallBarWithTimePeriodTwo(aggregator, "2020-02-05T15:00:00.000000000", 5);
        Bar fifthSmallBar = smallBarWithTimePeriodTwo(aggregator, "2020-02-05T17:00:00.000000000", 1);

        Bar expectedFirstBigBar = bigBarWithTimePeriodSix(aggregator, "2020-02-05T06:00:00.000000000", 1, 1, 1, 1, 1);
        Bar expectedSecondBigBar = bigBarWithTimePeriodSix(aggregator, "2020-02-05T12:00:00.000000000", 1, 5, 1, 5, 8);
        Bar expectedThirdBigBar = bigBarWithTimePeriodSix(aggregator, "2020-02-05T18:00:00.000000000", 1, 1, 1, 1, 1);

        aggregator.addBar(firstSmallBar);
        aggregator.addBar(secondSmallBar);
        aggregator.addBar(thirdSmallBar);
        aggregator.addBar(forthSmallBar);
        aggregator.addBar(fifthSmallBar);

        List<Bar> allBigBars = aggregator.getBarData();
        Assertions.assertThat(allBigBars)
                .hasSize(3)
                .containsSequence(expectedFirstBigBar, expectedSecondBigBar, expectedThirdBigBar);

    }

    @Test
    public void shouldAggregateSmallBarsIntoTwoBigBarsWithTimeGapBetweenThem() {
        aggregator = new AggregatedBarSeries(Duration.ofHours(6));
        Bar firstSmallBar = smallBarWithTimePeriodTwo(aggregator, "2020-02-05T07:12:59.999999999", 1);
        Bar secondSmallBar = smallBarWithTimePeriodTwo(aggregator, "2020-02-05T17:00:00.000000000", 1);

        Bar expectedFirstBigBar = bigBarWithTimePeriodSix(aggregator, "2020-02-05T06:00:00.000000000", 1, 1, 1, 1, 1);
        Bar expectedSecondBigBar = bigBarWithTimePeriodSix(aggregator, "2020-02-05T18:00:00.000000000", 1, 1, 1, 1, 1);

        aggregator.addBar(firstSmallBar);
        aggregator.addBar(secondSmallBar);

        List<Bar> allBigBars = aggregator.getBarData();
        Assertions.assertThat(allBigBars)
                .hasSize(2)
                .containsSequence(expectedFirstBigBar, expectedSecondBigBar);
    }

    @Test
    public void shouldFillBigBarWithSmallBarsTimePeriodOneSecond() {
        int expectedVolumeAndAmountCount = 1800; // 1800sec = 30min = 60sec * 30
        Duration smallBarTimePeriod = Duration.ofSeconds(1);
        Duration bigBarTimePeriod = Duration.ofMinutes(30);
        aggregator = new AggregatedBarSeries(bigBarTimePeriod);

        int i = 1;
        ZonedDateTime smallBarEndTime = parseZdt("2020-02-05T03:00:00.000000000", SMALL_BAR_ZONE);
        while (i++ <= expectedVolumeAndAmountCount) {
            smallBarEndTime = smallBarEndTime.plusSeconds(1);
            Bar firstSmallBar = createBar(smallBarEndTime,
                    1, 1, 1, 1, 1, 1, smallBarTimePeriod, aggregator);
            aggregator.addBar(firstSmallBar);
        }

        Bar expectedBigBar = bigBarWithCustomTimePeriod(aggregator, bigBarTimePeriod,
                "2020-02-05T00:30:00.000000000", 1, 1, 1, 1, expectedVolumeAndAmountCount);

        List<Bar> allBigBars = aggregator.getBarData();
        Assertions.assertThat(allBigBars)
                .hasSize(1)
                .containsOnly(expectedBigBar);
    }

    @Test
    public void shouldNotFailIfSmallBarTimePeriodEqualBigBarTimePeriod() {
        Duration timePeriod = Duration.ofHours(24);
        aggregator = new AggregatedBarSeries(timePeriod);
        Bar smallBar = smallBarWithCustomTimePeriod(aggregator, timePeriod, "2020-03-05T03:00:00.000000000",
                1, 1, 1, 1, 1);
        Bar expectedBigBar = bigBarWithCustomTimePeriod(aggregator, timePeriod, "2020-02-05T00:00:00.000000000",
                1, 1, 1, 1, 1);

        aggregator.addBar(smallBar);

        Bar resultBigBar = aggregator.getLastBar();
        Assert.assertEquals(expectedBigBar, resultBigBar);
    }

    @Test
    public void shouldPutSmallBarInCorrectBigBarInUTC() {
        Duration firstBigBarTimePeriod = Duration.between(
                LocalTime.of(17, 0, 0, 0),
                LocalTime.of(23, 59, 59, 999999999)
        ).plusNanos(1);
        aggregator = new AggregatedBarSeries(Duration.ofHours(17));
        Bar smallBar = smallBarWithTimePeriodSeven(aggregator, "2020-02-05T09:12:59.999999999", 1);
        Bar expectedBigBar = bigBarWithCustomTimePeriod(aggregator, firstBigBarTimePeriod,
                "2020-01-05T00:00:00.000000000", 1, 1, 1, 1, 1);
        aggregator.addBar(smallBar);

        List<Bar> result = aggregator.getBarData();
        Assertions.assertThat(result)
                .hasSize(1)
                .containsOnly(expectedBigBar);
    }

    @Test
    public void shouldReplaceLastSmallBarWhenAddBarCallWithReplaceTrue() {
        aggregator = new AggregatedBarSeries(Duration.ofHours(6));
        Bar firstSmallBar = smallBarWithTimePeriodOne(aggregator, "2020-02-05T09:00:00.0", 1);
        Bar secondSmallBar = smallBarWithTimePeriodOne(aggregator, "2020-02-05T10:00:00.0", 2);
        Bar thirdSmallBar = smallBarWithTimePeriodOne(aggregator, "2020-02-05T11:00:00.0", 3);

        Bar expectedBigBar = bigBarWithTimePeriodSix(aggregator, "2020-02-05T12:00:00.000000000", 3, 3, 3, 3, 3);

        aggregator.addBar(firstSmallBar);
        aggregator.addBar(secondSmallBar);
        aggregator.addBar(thirdSmallBar, true);

        Bar resultBigBar = aggregator.getLastBar();
        Assert.assertEquals(expectedBigBar, resultBigBar);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailIfSmallBarBeginsBeforePreviousSmallBarEnd() {
        aggregator = new AggregatedBarSeries(Duration.ofHours(6));
        Bar firstSmallBar = smallBarWithTimePeriodOne(aggregator, "2020-02-05T09:00:00.0", 1);
        Bar secondSmallBar = smallBarWithTimePeriodTwo(aggregator, "2020-02-05T10:00:00.0", 1);

        aggregator.addBar(firstSmallBar);
        aggregator.addBar(secondSmallBar);
    }

    @Test(expected = NullPointerException.class)
    public void shouldFailIfTimePeriodOfBigBarIsNull() {
        aggregator = new AggregatedBarSeries(null);
    }

    @Test(expected = NullPointerException.class)
    public void shouldFailIfSmallBarNull() {
        aggregator = new AggregatedBarSeries(Duration.ofHours(6));
        aggregator.addBar(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailIfTimePeriodOfBigBarMoreThan24Hours() {
        aggregator = new AggregatedBarSeries(Duration.ofHours(27));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailIfTimePeriodOfBigBarLessThanOneSecond() {
        aggregator = new AggregatedBarSeries(Duration.ofMillis(100));
    }

}