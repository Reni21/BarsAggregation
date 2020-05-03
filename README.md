## Trading Bars Aggregator

The ta4j library is an open source library for technical analysis. It provides the basic components for
creation, evaluation and execution of trading strategies.
https://ta4j.github.io/ta4j-wiki/Getting-started.html

This exercise requires you to understand the concept of Time series and Bars.
https://ta4j.github.io/ta4j-wiki/Time-series-and-bars.html

The task is to create a specialized implementation of the TimeSeries interface with the purpose of
aggregating lower timeframe bars into higher timeframe bars. By aggregation we mean merging
multiple lower timeframe bars into one higher timeframe bar and calculating the open, high, low and
close price and the cumulative volume and trade count for the aggregated bar based on the values
from the lower timeframe bars. The aggregation timeframe will be passed to the constructor of the
class as a Duration object, and the lower timeframe bars of the source data will be fed into the class
via the TimeSeries#addBar(Bar, boolean) method.
The aggregation algorithm should produce the same bar boundaries regardless of the start time of the
first lower timeframe bar. In order to achieve this, we require that aggregated bars don’t overlap day
boundaries. In other words, a new aggregated bar has to be created at every midnight boundary
(according to UTC time zone), even if the last bar of the previous day is shorter than the aggregation
timeframe.