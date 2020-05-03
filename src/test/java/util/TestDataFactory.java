package util;

import aggregator.AggregatedBarSeries;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

public class TestDataFactory {
    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-dd-MM'T'HH:mm:ss.SSSSSSSSS");
    public static final ZoneId SMALL_BAR_ZONE = ZoneId.of("Europe/Kiev");
    private static final ZoneId BIG_BAR_ZONE = ZoneOffset.UTC;

    private static final Duration TIME_PERIOD_ONE = Duration.ofHours(1);
    private static final Duration TIME_PERIOD_TWO = Duration.ofHours(2);
    private static final Duration TIME_PERIOD_SIX = Duration.ofHours(6);
    private static final Duration TIME_PERIOD_SEVEN = Duration.ofHours(7);

    public static Bar smallBarWithTimePeriodOne(AggregatedBarSeries aggregator, String time, int dataVal) {
        return createBar(parseZdt(time, SMALL_BAR_ZONE),
                dataVal, dataVal, dataVal, dataVal, dataVal, dataVal, TIME_PERIOD_ONE,
                aggregator
        );
    }

    public static Bar smallBarWithTimePeriodTwo(AggregatedBarSeries aggregator, String time, int dataVal) {
        return createBar(parseZdt(time, SMALL_BAR_ZONE),
                dataVal, dataVal, dataVal, dataVal, dataVal, dataVal, TIME_PERIOD_TWO, aggregator
        );
    }

    public static Bar smallBarWithTimePeriodSeven(AggregatedBarSeries aggregator, String time, int dataVal) {
        return createBar(parseZdt(time, SMALL_BAR_ZONE),
                dataVal, dataVal, dataVal, dataVal, dataVal, dataVal, TIME_PERIOD_SEVEN, aggregator
        );
    }

    public static Bar bigBarWithTimePeriodSix(AggregatedBarSeries aggregator, String time,
                                              int oP, int hP, int lP, int cP, int volumeAmount) {
        return createBar(parseZdt(time, BIG_BAR_ZONE),
                oP, hP, lP, cP, volumeAmount, volumeAmount, TIME_PERIOD_SIX, aggregator
        );
    }

    public static Bar bigBarWithCustomTimePeriod(AggregatedBarSeries aggregator, Duration timePeriod,
                                                 String time, int oP, int hP, int lP, int cP, int volumeAmount) {
        return createBar(parseZdt(time, BIG_BAR_ZONE),
                oP, hP, lP, cP, volumeAmount, volumeAmount, timePeriod, aggregator
        );
    }

    public static Bar smallBarWithCustomTimePeriod(AggregatedBarSeries aggregator, Duration timePeriod,
                                                   String time, int oP, int hP, int lP, int cP, int volumeAmount) {
        return createBar(parseZdt(time, SMALL_BAR_ZONE),
                oP, hP, lP, cP, volumeAmount, volumeAmount, timePeriod, aggregator
        );
    }

    public static Bar createBar(ZonedDateTime smallBarEndTime,
                                double openPrice, double highPrice, double lowPrice, double closePrice,
                                double volume, int amount, Duration duration, AggregatedBarSeries aggregator) {
        return BaseBar.builder()
                .endTime(smallBarEndTime)
                .timePeriod(duration)
                .openPrice(aggregator.numOf(openPrice))
                .highPrice(aggregator.numOf(highPrice))
                .lowPrice(aggregator.numOf(lowPrice))
                .closePrice(aggregator.numOf(closePrice))
                .volume(aggregator.numOf(volume))
                .amount(aggregator.numOf(amount))
                .build();
    }

    public static ZonedDateTime parseZdt(String zdtString, ZoneId zone) {
        String ns = zdtString.substring(zdtString.lastIndexOf(".") + 1);
        String effectiveZdtStr =
                (ns.length() < 9)
                        ? zdtString + String.join("", Collections.nCopies(9 - ns.length(), "0"))
                        : zdtString;
        return LocalDateTime.parse(effectiveZdtStr, DT_FORMAT).atZone(zone);
    }
}
