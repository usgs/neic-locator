package gov.usgs.locator;

import gov.usgs.locaux.SlabDepth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BayesianDepth implements Comparable<BayesianDepth> {

  /** Earthquake depth in kilometers. */
  private double depth;

  /**
   * Lower (deeper) bound for the earthquake depth in kilometers. Note that this may mean slightly
   * different things for different sources.
   */
  private double lower;

  /**
   * Upper (shallower) bound for the earthquake depth in kilometers. Note that this may mean
   * slightly different things for different sources.
   */
  private double upper;

  /** Standard deviation of the earthquake depth in kilometers. */
  private double spread;

  /** Source of this depth statistic. */
  private DepthSource source;

  /** Spread inflation factor. Note that this is only used if the interpolation is incomplete. */
  private double factor = 1d;

  /** Private logging object. */
  private static final Logger LOGGER = LogManager.getLogger(BayesianDepth.class.getName());

  /** @return Earthquake depth in km */
  public double getDepth() {
    return depth;
  }

  /** @return Lower (shallower) bound for earthquake depth in km */
  public double getLowerBound() {
    return lower;
  }

  /** @return Upper (deeper) bound for earthquake depth in km */
  public double getUpperBound() {
    return upper;
  }

  /** @return Standard deviation of the earthquake depth in km */
  public double getSpread() {
    return spread;
  }

  /** @param spread Depth error in kilometers */
  public void setSpread(double spread) {
    this.spread = spread;
  }

  /** @return Source of the Bayesian statistic */
  public DepthSource getSource() {
    return source;
  }

  /** @param source Depth source enumeration */
  public void setSource(DepthSource source) {
    this.source = source;
  }

  /**
   * Construct this object with all the data it will ever need.
   *
   * @param depth Earthquake depth in km
   * @param spread Standard deviation of the earthquake depth in km
   * @param source Source of the Bayesian statistic
   */
  public BayesianDepth(double depth, double spread, DepthSource source) {
    this.depth = depth;
    this.spread = spread;
    this.source = source;
    lower = depth - spread;
    upper = depth + spread;
    factor = 1d;
  }

  /**
   * Construct this object with all the data it will ever need.
   *
   * @param depth Earthquake depth in km
   * @param lower Lower (shallower) bound for earthquake depth in km
   * @param upper Upper (deeper) bound for earthquake depth in km
   * @param spread Standard deviation of the earthquake depth in km
   * @param source Source of the Bayesian statistic
   */
  public BayesianDepth(
      double depth, double lower, double upper, double spread, DepthSource source) {
    this.depth = depth;
    this.lower = lower;
    this.upper = upper;
    this.spread = spread;
    this.source = source;
    factor = 1d;
  }

  /**
   * Construct this object with all the data it will ever need.
   *
   * @param depth Earthquake depth in km
   * @param lower Lower (shallower) bound for earthquake depth in km
   * @param upper Upper (deeper) bound for earthquake depth in km
   * @param source Source of the Bayesian statistic
   * @param minSpread Minimum acceptable value for spread in km
   */
  public BayesianDepth(
      double depth, double lower, double upper, DepthSource source, double minSpread) {
    this.depth = depth;
    this.lower = lower;
    this.upper = upper;
    if (Double.isNaN(minSpread)) {
      this.spread = Math.max(lower, upper);
    } else {
      this.spread = Math.max(Math.max(depth - lower, upper - depth), minSpread);
    }
    this.source = source;
    factor = 1d;
  }

  /**
   * Construct this object from a slab depth object.
   *
   * @param slabDepth SlabDepth object
   */
  public BayesianDepth(SlabDepth slabDepth) {
    depth = slabDepth.getEqDepth();
    lower = slabDepth.getLower();
    upper = slabDepth.getUpper();
    spread = Math.max(depth - lower, upper - depth);
    source = DepthSource.SLABMODEL;
    factor = 1d;
  }

  /**
   * Create an empty BayesianDepth to be filled in using the setByIndex method.
   *
   * @param source A DepthSource object identifying the depth source
   */
  public BayesianDepth(DepthSource source) {
    depth = Double.NaN;
    lower = Double.NaN;
    upper = Double.NaN;
    spread = Double.NaN;
    this.source = source;
    factor = 1d;
  }

  /**
   * Treat [depth, lower, upper, spread] as a virtual array for the purpose of getting and setting.
   *
   * @param index Virtual array index (0 = mean depth, 1 = lower, 2 = upper, and 3 = spread)
   * @return Indexed depth parameter
   */
  public double getByIndex(int index) {
    switch (index) {
      case 0:
        return depth;
      case 1:
        return lower;
      case 2:
        return upper;
      case 3:
        return spread;
      default:
        return Double.NaN;
    }
  }

  /**
   * Treat [depth, lower, upper, spread] as a virtual array for the purpose of getting and setting.
   *
   * @param index Virtual array index (0 = mean depth, 1 = lower, 2 = upper, and 3 = spread)
   * @param param Value to set the indexed depth parameter
   */
  public void setByIndex(int index, double param) {
    switch (index) {
      case 0:
        depth = param;
        break;
      case 1:
        lower = param;
        break;
      case 2:
        upper = param;
        break;
      case 3:
        spread = param;
        break;
      default:
        LOGGER.warn("BayesianDepth.setByIndex: there is no case > 3!");
    }
  }

  /**
   * When we're on the edge of a structure and have less than the number of samples for
   * interpolation, compensate by inflating the spread arbitrarily.
   *
   * @param factor Inflation factor
   */
  public void inflateSpread(double factor) {
    this.factor = factor;
    spread *= this.factor;
  }

  /**
   * If the spread has been overridden, it may need to be inflated again.
   *
   * @param spread Overridden spread
   * @return Inflated overridden spread
   */
  public double reInflateSpread(double spread) {
    return factor * spread;
  }

  @Override
  public int compareTo(BayesianDepth bayes) {
    if (this.depth > bayes.getDepth()) {
      return +1;
    } else if (this.depth < bayes.getDepth()) {
      return -1;
    } else {
      return 0;
    }
  }

  @Override
  public String toString() {
    return String.format("%5.1f +/- %5.1f [%5.1f, %5.1f] %s", depth, spread, lower, upper, source);
  }
}
