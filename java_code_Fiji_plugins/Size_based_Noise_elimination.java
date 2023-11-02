import ij.*;
import ij.plugin.filter.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.macro.*;
import ij.gui.GenericDialog.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener; 

public class Size_based_Noise_elimination implements PlugInFilter{
	ImagePlus imp;
	//	String origi;
	//	String origi = imp.getTitle();
	int nslice=0, red1, green1, blue1;
	int maxvalue=0;
	int measuresize=0;
	int startval=0;
	int pixsum=0;
	int background1=0;
	int signal1=0;
	int pix = 0;
	
	public int setup(String arg, ImagePlus imp){
		IJ.register (Size_based_Noise_elimination.class);
		if (IJ.versionLessThan("1.32c")){
			IJ.showMessage("Error", "Please Update ImageJ.");
			return 0;
		}
		
		int[] wList = WindowManager.getIDList();
		if (wList==null) {
			IJ.error("No images are open.");
			return 0;
		}
		//	IJ.log(" wList;"+String.valueOf(wList));
		imp = WindowManager.getCurrentImage();
		this.imp = imp;
		if(imp.getType()!=imp.GRAY8 && imp.getType()!=imp.GRAY16 && imp.getType()!=imp.COLOR_RGB){
			IJ.showMessage("Error", "Plugin requires 8- or 16-bit or RGB image");
			return 0;
		}
		//if(imp.getType()==imp.GRAY8)
		//new ImageConverter(imp).convertToGray16();
		
		if(imp.getType()==imp.COLOR_RGB)
		maxvalue=255;
		
		if(imp.getType()==imp.GRAY8)
		maxvalue=255;
		
		if(imp.getType()==imp.GRAY16)
		maxvalue=66535;
		
		startval = (int)Prefs.get("Thresholding_noise.int", 10);
		measuresize = (int)Prefs.get("measuresize.int", 5);
		
		GenericDialog gd = new GenericDialog("Size based Noise cancelling");
		gd.addSlider("ignore less than this value", 0, maxvalue, startval);
		gd.addSlider("less than this value is the noise", 1, 6, measuresize);

		gd.showDialog();
		if(gd.wasCanceled()){
			return 0;
		}
		
		startval = (int)gd.getNextNumber();
		measuresize = (int)gd.getNextNumber();

		
		Prefs.set("Thresholding_noise.int", startval);
		Prefs.set("measuresize.int", measuresize);
		
		return DOES_8G+DOES_16+DOES_RGB;
	}
	
	public void run(ImageProcessor ip){
		//	String ff = ip.getTitle();
		
		nslice = imp.getNSlices();

		int ww = ip.getWidth() ;
		int hh = ip.getHeight();
		int sumpx = ip.getPixelCount();
		ImageStack stack = imp.getStack();
		//		ImageProcessor ip2 = ip.duplicate();
		
		for(int sliceposi=1; sliceposi<=nslice; sliceposi++){
			ip = stack.getProcessor(sliceposi);
			
			int step=1;
			while(step<=2){
				for(int xx=0; xx<sumpx-(4*ww); xx+=ww){//horizontal line to vertical grow
					
					for(int i=xx; i<xx+ww-4; i++){
						
						pixsum=0;
						background1=0;
						signal1=0;
						
						for(int ii=i; ii<=i+4; ii++){//within 5x5 px window horizontal
							for(int iv=ii; iv<=ii+(4*ww); iv+=ww){//within 5x5 px window vertical
								pix = ip.get (iv);	
								
								if(imp.getType()==imp.COLOR_RGB){
									int red1 = (pix>>>16) & 0xff;//mask
									int green1 = (pix>>>8) & 0xff;//mask
									int blue1 = pix & 0xff;//mask
									
									if(red1>=green1 && red1>=blue1)
									pix=red1;
									else if(green1>=red1 && green1>=blue1)
									pix=green1;
									else if(blue1>=red1 && blue1>=green1)
									pix=blue1;
								}
								
								pixsum=pixsum+1;

								if(pix>=startval){
									if(pixsum<7 || pixsum==10 || pixsum==11 || pixsum==15 || pixsum==16 || pixsum>19)
										background1=background1+1;
									else
									signal1=signal1+1;
									
								}//if(pix>=startval){
							}////vertical
						}//horizontal
						
						
						if (background1==0){
							if (signal1>0){
								if (signal1<=measuresize){
									for(int iii=i+1; iii<=i+3; iii++){//horizontal
										for(int ivv=iii+ww; ivv<=iii+(3*ww); ivv+=ww){//vertical
											ip.set (ivv, 0);
										}
									}
								}
							}
						}
					}
				}//for(int i=0; i<sumpx-(4*ww); i++){
				step=step+1;	
			}//	while(step<=2){
			IJ.showProgress (sliceposi, nslice);
		}
		imp.show();
	}
}






