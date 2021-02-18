package fiji.plugin.trackmate.gui.wizard;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.ManualDetectorFactory;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.wizard.descriptors.ConfigureViewsDescriptor;
import fiji.plugin.trackmate.tracking.ManualTrackerFactory;
import ij.ImageJ;
import ij.ImagePlus;

public class ManualTrackingWizardPlugIn extends NewTrackMateWizardPlugin
{

	@Override
	protected WizardSequence createSequence( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings )
	{
		final WizardSequence sequence = super.createSequence( trackmate, selectionModel, displaySettings );
		sequence.setCurrent( ConfigureViewsDescriptor.KEY );
		return sequence;
	}

	@SuppressWarnings( "rawtypes" )
	@Override
	protected Settings createSettings( final ImagePlus imp )
	{
		final Settings lSettings = super.createSettings( imp );
		// Manual detection
		lSettings.detectorFactory = new ManualDetectorFactory();
		lSettings.detectorSettings = lSettings.detectorFactory.getDefaultSettings();
		// Manual tracker
		lSettings.trackerFactory = new ManualTrackerFactory();
		lSettings.trackerSettings = lSettings.trackerFactory.getDefaultSettings();
		return lSettings;
	}

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		new ManualTrackingWizardPlugIn().run( "samples/Merged.tif" );
	}
}
