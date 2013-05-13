package fiji.plugin.trackmate;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * This class represents the part of the {@link TrackMateModel} that is in charge 
 * of dealing with spot features and track features.
 * @author Jean-Yves Tinevez, 2011, 2012
 *
 */
public class FeatureModel {

	/*
	 * FIELDS
	 */

	private Collection<String> trackFeatures = new HashSet<String>();
	private HashMap<String, String> trackFeatureNames = new HashMap<String, String>();
	private HashMap<String, String> trackFeatureShortNames = new HashMap<String, String>();
	private HashMap<String, Dimension> trackFeatureDimensions = new HashMap<String, Dimension>();
	/**
	 * Feature storage. We use a Map of Map as a 2D Map. The list maps each
	 * track to its feature map. The feature map maps each
	 * feature to the double value for the specified feature.
	 */
	protected Map<Integer, Map<String, Double>> trackFeatureValues =  new ConcurrentHashMap<Integer, Map<String, Double>>();

	/**
	 * Feature storage for edges.
	 */
	protected ConcurrentHashMap<DefaultWeightedEdge, ConcurrentHashMap<String, Double>> edgeFeatureValues = 
			new ConcurrentHashMap<DefaultWeightedEdge, ConcurrentHashMap<String, Double>>();

	private Collection<String> edgeFeatures = new HashSet<String>();
	private HashMap<String, String> edgeFeatureNames = new HashMap<String, String>();
	private HashMap<String, String> edgeFeatureShortNames = new HashMap<String, String>();
	private HashMap<String, Dimension> edgeFeatureDimensions = new HashMap<String, Dimension>();

	private Collection<String> spotFeatures = new HashSet<String>();
	private Map<String,String> spotFeatureNames = new HashMap<String, String>();
	private Map<String,String> spotFeatureShortNames = new HashMap<String, String>();
	private Map<String,String> spotFeatureDimensions = new HashMap<String, String>();

	private TrackMateModel model;

	/*
	 * CONSTRUCTOR
	 */

	FeatureModel(TrackMateModel model) {
		this.model = model;
	}

	/*
	 * METHODS
	 */
	

	/**
	 * Returns a new double array with all the values for the specified track feature.
	 * @param trackFeature the track feature to parse. Throw an {@link IllegalArgumentException}
	 * if the feature is unknown.
	 * @param filteredOnly if <code>true</code>, will only include filtered tracks, 
	 * all the tracks otherwise.
	 * @return a new <code>double[]</code>, one element per track.
	 */
	public double[] getTrackFeatureValues(String trackFeature, boolean filteredOnly) {
		if (!trackFeatures.contains(trackFeature)) {
			throw new IllegalArgumentException("Unknown track feature: " + trackFeature);
		}
		final Set<Integer> keys;
		if (filteredOnly) {
			keys = model.getTrackModel().getFilteredTrackIDs();
		} else {
			keys = model.getTrackModel().getTrackIDs();
		}
		double[] val = new double[keys.size()];
		int index = 0;
		for (Integer trackID : keys) {
			val[index++] = getTrackFeature(trackID, trackFeature).doubleValue(); 
		}
		return val;
	}
	
	/**
	 * Returns a new double array with all the values for the specified edge feature.
	 * @param edgeFeature the track feature to parse. Throw an {@link IllegalArgumentException}
	 * if the feature is unknown.
	 * @param filteredOnly if <code>true</code>, will only include edges in filtered tracks, 
	 * in all the tracks otherwise.
	 * @return a new <code>double[]</code>, one element per edge.
	 */
	public double[] getEdgeFeatureValues(String edgeFeature, boolean filteredOnly) {
		if (!edgeFeatures.contains(edgeFeature)) {
			throw new IllegalArgumentException("Unknown edge feature: " + edgeFeature);
		}
		final Set<Integer> keys;
		if (filteredOnly) {
			keys = model.getTrackModel().getFilteredTrackIDs();
		} else {
			keys = model.getTrackModel().getTrackIDs();
		}
		int nvals = 0;
		for (Integer trackID : keys) {
			nvals += model.getTrackModel().getTrackEdges(trackID).size();
		}
		
		double[] val = new double[nvals];
		int index = 0;
		for (Integer trackID : keys) {
			for (DefaultWeightedEdge edge : model.getTrackModel().getTrackEdges(trackID)) {
				val[index++] = getEdgeFeature(edge, edgeFeature).doubleValue(); 
			}
		}
		return val;
	}



	/*
	 * EDGE FEATURES
	 */

	/**
	 * Stores a numerical feature for an edge of this model. The specified edge must exist in 
	 * the model, and the feature must have been declared. 
	 * @param edge  the edge whose features to update.
	 * @param feature  the feature.
	 * @param value  the feature value
	 * @return <code>true</code> if the specified edge and feature belong to the model and had its feature
	 * updated; <code>false</code> otherwise. 
	 */
	public synchronized boolean putEdgeFeature(DefaultWeightedEdge edge, final String feature, final Double value) {
		if (!model.getTrackModel().edgeSet().contains(edge) || !edgeFeatures.contains(feature)) {
			return false;
		}
		
		ConcurrentHashMap<String, Double> map = edgeFeatureValues.get(edge);
		if (null == map) {
			map = new ConcurrentHashMap<String, Double>();
			edgeFeatureValues.put(edge, map);
		}
		map.put(feature, value);
		return true;
	}

	public Double getEdgeFeature(DefaultWeightedEdge edge, final String featureName) {
		ConcurrentHashMap<String, Double> map = edgeFeatureValues.get(edge);
		if (null == map) {
			return null;
		}
		return map.get(featureName);
	}

	/**
	 * Returns edge features as declared in this model. 
	 * @return the edge features.
	 */ 
	public Collection<String> getEdgeFeatures() {
		return edgeFeatures;
	}
	

	/**
	 * Resets the edge features, names, short names, dimensions and values. 
	 * New features will have to be declared prior to storing them.
	 */
	public void clearEdgeFeatures() {
		edgeFeatures.clear();
		edgeFeatureNames.clear();
		edgeFeatureShortNames.clear();
		edgeFeatureDimensions.clear();
		edgeFeatureValues.clear();
	}
	
	/**
	 * Declares edge features, by specifying their name, short name and dimension.
	 * An {@link IllegalArgumentException} will be thrown if any of the map misses
	 * a feature.
	 * @param features  the list of edge features to register. 
	 * @param featureNames  the name of these features.
	 * @param featureShortNames  the short name of these features.
	 * @param featureDimensions  the dimension of these features.
	 */
	public void declareEdgeFeatures(Collection<String> features, Map<String, String> featureNames, 
			Map<String, String> featureShortNames, Map<String, Dimension> featureDimensions) {
		edgeFeatures.addAll(features);
		for (String feature : features) {
			
			String name = featureNames.get(feature);
			if (null == name) {
				throw new IllegalArgumentException("Feature " + feature + " misses a name.");
			}
			edgeFeatureNames.put(feature, name);
			
			String shortName = featureShortNames.get(feature);
			if (null == shortName) {
				throw new IllegalArgumentException("Feature " + feature + " misses a short name.");
			}
			edgeFeatureShortNames.put(feature, shortName);
			
			Dimension dimension = featureDimensions.get(feature);
			if (null == dimension) {
				throw new IllegalArgumentException("Feature " + feature + " misses a dimension.");
			}
		}
	}

	/**
	 * Returns the name mapping of the edge features that are dealt with in this model.
	 * @return the map of edge feature names.
	 */
	public Map<String, String> getEdgeFeatureNames() {
		return edgeFeatureNames;
	}

	/**
	 * Returns the short name mapping of the edge features that are dealt with in this model.
	 * @return the map of edge short names.
	 */
	public Map<String, String> getEdgeFeatureShortNames() {
		return edgeFeatureShortNames;
	}

	/**
	 * Returns the dimension mapping of the edge features that are dealt with in this model.
	 * @return the map of edge feature dimensions.
	 */
	public Map<String, Dimension> getEdgeFeatureDimensions() {
		return edgeFeatureDimensions;
	}


	/*
	 * TRACK FEATURES
	 */

	/**
	 * Returns the track features that are dealt with in this model.
	 */
	public Collection<String> getTrackFeatures() {
		return trackFeatures;
	}
	
	/**
	 * Resets the track features, names, short names, dimensions and values. 
	 * New features will have to be declared prior to storing them.
	 */
	public void clearTrackFeatures() {
		trackFeatures.clear();
		trackFeatureNames.clear();
		trackFeatureShortNames.clear();
		trackFeatureDimensions.clear();
		trackFeatureValues.clear();
	}
	
	/**
	 * Declares track features, by specifying their names, short name and dimension.
	 * An {@link IllegalArgumentException} will be thrown if any of the map misses
	 * a feature.
	 * @param features  the list of track feature to register. 
	 * @param featureNames  the name of these features.
	 * @param featureShortNames  the short name of these features.
	 * @param featureDimensions  the dimension of these features.
	 */
	public void declareTrackFeatures(Collection<String> features, Map<String, String> featureNames, 
			Map<String, String> featureShortNames, Map<String, Dimension> featureDimensions) {
		trackFeatures.addAll(features);
		for (String feature : features) {
			
			String name = featureNames.get(feature);
			if (null == name) {
				throw new IllegalArgumentException("Feature " + feature + " misses a name.");
			}
			trackFeatureNames.put(feature, name);
			
			String shortName = featureShortNames.get(feature);
			if (null == shortName) {
				throw new IllegalArgumentException("Feature " + feature + " misses a short name.");
			}
			trackFeatureShortNames.put(feature, shortName);
			
			Dimension dimension = featureDimensions.get(feature);
			if (null == dimension) {
				throw new IllegalArgumentException("Feature " + feature + " misses a dimension.");
			}
		}
	}

	/**
	 * Returns the name mapping of the track features that are dealt with in this model.
	 */
	public Map<String, String> getTrackFeatureNames() {
		return trackFeatureNames;
	}

	/**
	 * Returns the short name mapping of the track features that are dealt with in this model.
	 * @return
	 */
	public Map<String, String> getTrackFeatureShortNames() {
		return trackFeatureShortNames;
	}

	/**
	 * Returns the dimension mapping of the track features that are dealt with in this model.
	 */
	public Map<String, Dimension> getTrackFeatureDimensions() {
		return trackFeatureDimensions;
	}

	/**
	 * Stores a track numerical feature. The track ID must exist in the model and 
	 * the specified feature must have been declared.
	 * @param trackID  the ID of the track. It must be an existing track ID.
	 * @param feature  the feature.
	 * @param value  the feature value.
	 * @return <code>true</code> if the specified track ID and feature was found in the model and the 
	 * specified feature has been updated, <code>false</code> otherwise.
	 */
	public synchronized boolean putTrackFeature(final Integer trackID, final String feature, final Double value) {
		
		// We use getTrackSpots, because it is seldom recomputed.
		if (!model.getTrackModel().getTrackSpots().keySet().contains(trackID) || !trackFeatures.contains(feature)) {
			return false;
		}
		
		Map<String, Double> trackFeatureMap = trackFeatureValues.get(trackID);
		if (null == trackFeatureMap) {
			trackFeatureMap = new HashMap<String, Double>(trackFeatures.size());
			trackFeatureValues.put(trackID, trackFeatureMap);
		}
		trackFeatureMap.put(feature, value);
		return true;
	}

	/**
	 * @return the numerical value of the specified track feature for the specified track.
	 * @param trackID the track ID to quest.
	 * @param feature the desired feature.
	 */
	public Double getTrackFeature(final Integer trackID, final String feature) {
		Map<String, Double> valueMap = trackFeatureValues.get(trackID);
		return valueMap.get(feature);
	}

	public Map<String, double[]> getTrackFeatureValues() {
		final Map<String, double[]> featureValues = new HashMap<String, double[]>();
		Double val;
		int nTracks = model.getTrackModel().getNTracks();
		for (String feature : trackFeatures) {
			// Make a double array to comply to JFreeChart histograms
			boolean noDataFlag = true;
			final double[] values = new double[nTracks];
			int index = 0;
			for (Integer trackID : model.getTrackModel().getTrackIDs()) {
				val = getTrackFeature(trackID, feature);
				if (null == val)
					continue;
				values[index++] = val;
				noDataFlag = false;
			}

			if (noDataFlag)
				featureValues.put(feature, new double[0]); // Empty array to signal no data
			else
				featureValues.put(feature, values);
		}
		return featureValues;
	}
	
	/*
	 * SPOT FEATURES
	 * the spot features are stored in the Spot object themselves, but we declare them here.
	 */
	

	/**
	 * Resets the spot features, names, short names and dimensions.
	 */
	public void clearSpotFeatures() {
		spotFeatures .clear();
		spotFeatureNames .clear();
		spotFeatureShortNames .clear();
		spotFeatureDimensions .clear();
	}
	
	/**
	 * Declares spot features, by specifying their names, short name and dimension.
	 * An {@link IllegalArgumentException} will be thrown if any of the map misses
	 * a feature.
	 * @param features  the list of spot feature to register. 
	 * @param featureNames  the name of these features.
	 * @param featureShortNames  the short name of these features.
	 * @param featureDimensions  the dimension of these features.
	 */
	public void declareSpotFeatures(Collection<String> features, Map<String, String> featureNames, 
			Map<String, String> featureShortNames, Map<String, Dimension> featureDimensions) {
		spotFeatures.addAll(features);
		for (String feature : features) {
			
			String name = featureNames.get(feature);
			if (null == name) {
				throw new IllegalArgumentException("Feature " + feature + " misses a name.");
			}
			spotFeatureNames.put(feature, name);
			
			String shortName = featureShortNames.get(feature);
			if (null == shortName) {
				throw new IllegalArgumentException("Feature " + feature + " misses a short name.");
			}
			spotFeatureShortNames.put(feature, shortName);
			
			Dimension dimension = featureDimensions.get(feature);
			if (null == dimension) {
				throw new IllegalArgumentException("Feature " + feature + " misses a dimension.");
			}
		}
	}

}
