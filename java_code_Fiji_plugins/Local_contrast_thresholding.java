import ij.*;
import ij.plugin.filter.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.macro.*;
import ij.gui.GenericDialog.*;
//import javax.swing.*;
//import javax.swing.event.ChangeEvent;
//import javax.swing.event.ChangeListener;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener; 
import java.util.concurrent.atomic.AtomicInteger;

public class Local_contrast_thresholding implements PlugInFilter
{
	ImagePlus imp;
	
	int pxrange;
	int scanoverlap;
	int run2 = 0; 
	String noisemethod;
	int maxVAL=0;
	int pix=0;
	int thread_num_=1;
	double twenty =0;
	double forty =0;
	double eighty =0;
	double zero =0;
	double mingray = 0;
	double maxgray = 0;
	
	public int setup(String arg, ImagePlus imp)
	{
		IJ.register (Local_contrast_thresholding.class);
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
		if(imp.getType()!=imp.GRAY8 && imp.getType()!=imp.GRAY16){
			IJ.showMessage("Error", "Plugin requires 8- or 16-bit image");
			return 0;
		}
		return DOES_8G+DOES_16;
	}
	
	public void run(ImageProcessor ip){
		//	String ff = ip.getTitle();
		
		if(imp.getType()==imp.GRAY8) 
		maxVAL=255;
		else if (imp.getType()==imp.GRAY16)
		maxVAL=65535;
		
		int pxrange = (int)Prefs.get("Scan box size.int", 50);
		//	int scanoverlap = (int)Prefs.get("Box overlap.int", 5);
		int noisenum=(int)Prefs.get("Noise canceling method.int",0);
		thread_num_=(int)Prefs.get("thread_num_.int",4);
		
		String []	thresholds = {"1px", "5px"};
		
		GenericDialog gd = new GenericDialog("Local thresholding");
		gd.addNumericField("Scan box size", pxrange,0);
		//	gd.addNumericField("Scan overlap px", scanoverlap,0);
		gd.addRadioButtonGroup("Noise canceling method", thresholds, 2, 2, thresholds[noisenum]);
		
		gd.addNumericField("CPU number", thread_num_, 0);
		
		if(imp.getType()==imp.GRAY8){
			twenty = (double)Prefs.get("twenty.double", 1.2);
			forty = (double)Prefs.get("forty.double", 1.05);
			eighty = (double)Prefs.get("eighty.double", 1.02);
			zero = (double)Prefs.get("zero.double", 1.2);
			mingray = (double)Prefs.get("mingray.double", 20);
			maxgray = (double)Prefs.get("maxgray.double", 100);
			
			gd.addNumericField("0-20 Gray value sensitivity;", zero,3);
			gd.addNumericField("<20-40 Gray value sensitivity;", twenty,3);
			gd.addNumericField("<40-80 Gray value sensitivity;", forty,3);
			gd.addNumericField(">80 Gray value sensitivity ;", eighty,3);
			gd.addSlider("Min gray value", 0, maxVAL, mingray);
			gd.addSlider("Max gray value for threshold", 0, maxVAL, maxgray);
		}else if(imp.getType()==imp.GRAY16){
			twenty = (double)Prefs.get("two100.double", 1.2);
			forty = (double)Prefs.get("six100.double", 1.05);
			eighty = (double)Prefs.get("senNi.double", 1.02);
			zero = (double)Prefs.get("zero16.double", 1.2);
			mingray = (double)Prefs.get("mingray16.double", 200);
			maxgray = (double)Prefs.get("maxgray16.double", 1000);
			
			gd.addNumericField("0-200 Gray value sensitivity;", zero,3);
			gd.addNumericField("<200-600 Gray value sensitivity;", twenty,3);
			gd.addNumericField("<600-1200 Gray value sensitivity;", forty,3);
			gd.addNumericField(">1200 Gray value sensitivity ;", eighty,3);
			gd.addSlider("Min gray value", 0, maxVAL, mingray);
			gd.addSlider("Max gray value for threshold", 0, maxVAL, maxgray);
		}
		gd.showDialog();
		if(gd.wasCanceled()){
			return;
		}
		pxrange=(int)gd.getNextNumber();
		//	scanoverlap=(int)gd.getNextNumber();
		noisemethod=(String)gd.getNextRadioButton();
		thread_num_ = (int)gd.getNextNumber();
		if(thread_num_ <= 0) thread_num_ = 1;
		
		zero=(double)gd.getNextNumber();
		twenty=(double)gd.getNextNumber();
		forty=(double)gd.getNextNumber();
		eighty=(double)gd.getNextNumber();
		mingray=(double)gd.getNextNumber();
		maxgray=(double)gd.getNextNumber();
		
		Prefs.set("Scan box size.int", pxrange);
		Prefs.set("thread_num_.int", thread_num_);
		//		Prefs.set("Box overlap.int", scanoverlap);
		if(imp.getType()==imp.GRAY8){
			Prefs.set("twenty.double", twenty);
			Prefs.set("forty.double", forty);
			Prefs.set("eighty.double", eighty);
			Prefs.set("zero.double", zero);
			Prefs.set("mingray.double", mingray);
			Prefs.set("maxgray.double", maxgray);
		}else if(imp.getType()==imp.GRAY16){
			Prefs.set("two100.double", twenty);
			Prefs.set("six100.double", forty);
			Prefs.set("senNi.double", eighty);
			Prefs.set("zero16.double", zero);
			Prefs.set("mingray16.double", mingray);
			Prefs.set("maxgray16.double", maxgray);
		}
		
		noisenum=1;
		if(noisemethod=="1px")
		noisenum=0;
		
		Prefs.set("Noise canceling method.int", noisenum);
		//	IJ.log(" noisemethod;"+String.valueOf(noisemethod));
		
		int ww = ip.getWidth() ;
		int hh = ip.getHeight();
		int sumpx = ip.getPixelCount();
		
		int range=(pxrange/2);
		
		
		ImageProcessor ip2 = ip.createProcessor(ww, hh);
		
		final AtomicInteger ai = new AtomicInteger(0);
		final Thread[] threads = newThreadArray();
		
		for (int ithread = 0; ithread < threads.length; ithread++) {
			// Concurrently run in as many threads as CPUs
			threads[ithread] = new Thread() {
				
				{ setPriority(Thread.NORM_PRIORITY); }
				
				public void run() {
					
					for(int xn1=ai.getAndIncrement(); xn1<ww; xn1 = ai.getAndIncrement()){
						
						if( IJ.escapePressed() )
						break;
						
						IJ.showProgress (xn1, ww);
						
						int xstart = xn1-range;
						if(xstart<0)
						xstart=1;
						
						int xend=xn1+range;
						if(xend>ww)
						xend=ww;
						
						for(int yn1=0; yn1<hh; yn1++){
							long sum = 0;
							double posipx = 0;
							double averange = 0;
							
							/////////////////////rectangle brightness measurement///////////////////////////////	
							
							int ystart=yn1-range;
							if(ystart<0)
							ystart=1;
							
							int yend=yn1+range;
							if(yend>hh)
							yend=hh;
							
							int count=0;
							
							//		if(n>range && n<sumpx-(ww*range)){
							for(int xn=xstart; xn<xend; xn++){
								for(int yn=ystart; yn<yend; yn++){
						
									pix=ip.get(xn,yn);
									count=count+1;
									
									if(pix>0)
									sum=sum+pix;
								}//	for(int yn=ystart; yn<yend; yn++){
							}//for(int xn=xstart; xn<xend; xn++){
							if(sum>0 && count>0)
							averange= sum / count;
							
							//IJ.log(" averange;"+String.valueOf(averange));
							if(averange>0){
								if(imp.getType()==imp.GRAY8){
							//		IJ.log("8bit");
									if(averange>20){
										if(averange<=40){
											posipx=averange*twenty;
											//			if(gap<40){
											//				posipx=averange*1.5;
											//			}
										}else if(averange<=80){
											posipx=averange*forty;//1.5
										}else if(averange>80){
											posipx=averange*eighty;//1.5
										}
									}else 
									posipx=averange*zero;
								}else if(imp.getType()==imp.GRAY16){
								//	IJ.log("16bit");
									if(averange>200){
										if(averange<=600){
											posipx=averange*twenty;
											//			if(gap<40){
											//				posipx=averange*1.5;
											//			}
										}else if(averange<=1200){
											posipx=averange*forty;//1.5
										}else if(averange>1200){
											posipx=averange*eighty;//1.5
										}
									}else 
									posipx=averange*zero;
								}
							}
							int pix1;
							
							/////////////////////Start: signal detection///////////////////////////////
							if(posipx>0){
								pix1= ip.get(xn1,yn1);
								
								if(pix1<mingray)
								ip2.set (xn1,yn1, 0);
								else if(maxgray < pix1)
								ip2.set (xn1,yn1, maxVAL);
								else if(posipx < pix1)
								ip2.set (xn1,yn1, maxVAL);
								
					//			IJ.log(" posipx;"+String.valueOf(posipx)+"  pix1; "+pix1);
							}//if(posipx>0){
						}//	for(yn1=0; yn1<hh; yn1++){
					}//for(int xn1=ai.getAndIncrement(); xn1<ww; xn1 = ai.getAndIncrement()){
			}};
		}//	for (int ithread = 0; ithread < threads.length; ithread++) {
		startAndJoin(threads);
		
		
		/////////////////////Noise canceling///////////////////////////////
		
		int WWnoise=0;
		int HHnoise=0;
		
		if (noisemethod=="5px"){
			WWnoise=ww-5;
			HHnoise=hh-5;
		}
		if (noisemethod=="1px"){
			WWnoise=ww-3;
			HHnoise=hh-3;
		}
		
		for (int nn=0; nn < WWnoise; nn++){
			
			IJ.showProgress (nn, WWnoise);
			IJ.showStatus("Noise canceling");
			
			for (int vvv=0; vvv < HHnoise; vvv++){
				
				////////////Start: noise fileter////////////////////////////////
				int counting = 0;//max 5*5=25;
				int posi2 = 0;
				int posicenter = 0;
				int posi5 = 0;
				int center1 = 0;
				int pixnnx = 0;
				
				if (noisemethod=="5px"){
					for(int nnx=nn; nnx<=nn+4; nnx++){
						for(int vvvy=vvv; vvvy<=vvv+4; vvvy++){
							pixnnx = ip2.get (nnx, vvvy);
							
							if(pixnnx<maxVAL){
								counting=counting+1;
								if (nnx==nn || nnx==nn+4){
									if (vvvy==vvv || vvvy==vvv+4){
										counting=counting-1;
									}
								}
							}
							
							if(pixnnx==maxVAL){
								if(nnx>nn){
									if(nnx<nn+4){
										if(vvvy>vvv){
											if(vvvy<vvv+4){
												posi5=posi5+1;
											}
										}
									} //if(nnx<nn+4){
								} //if(nnx>nn+1)	
							} 
						}
					} //for(nnx=nn; nnx<=nn+4; nnx++){
					
					if(posi5<=5){
						if(counting>16){
							//			if(center1==1){
							//print("noise1");
							ip2.set (nn+1, vvv+1, 0);
							ip2.set (nn+1, vvv+2, 0);
							ip2.set (nn+1, vvv+3, 0);
							ip2.set (nn+2, vvv+3, 0);
							ip2.set (nn+2, vvv+2, 0);
							ip2.set (nn+2, vvv+1, 0);
							ip2.set (nn+3, vvv+3, 0);
							ip2.set (nn+3, vvv+2, 0);
							ip2.set (nn+3, vvv+1, 0);
							//					}
						}
					}
				} //	if (noisemethod=="5px"){
				
				if (noisemethod=="1px"){
					for(int nnx=nn; nnx <= nn+2; nnx++){
						for(int vvvy=vvv; vvvy <= vvv+2; vvvy++){
							pixnnx=ip2.get (nnx, vvvy);
							
							if(pixnnx<maxVAL){
								posi5=posi5+1;
							}
							
							if(pixnnx==maxVAL){
								if(nnx==nn+1){ //center
									if(vvvy==vvv+1){
										center1=1;
									}
								}	//if(nnx==nn+1){
							} 
						}
					}
					if(posi5==8){
						if(center1==1){
							//print("noise1");
							ip2.putPixel (nn+1, vvv+1, 0);
						}
					}
				} //	if (noisemethod=="5px"){
			}
		} //for (int nn=0; nn<WWnoise; nn++)
		//IJ.log(" posipx;"+String.valueOf(posipx));
		ImagePlus dd = new ImagePlus("result_box size;"+String.valueOf(pxrange)+" noise filter px;"+noisemethod, ip2);
		dd.show();
		
		imp.getProcessor().resetMinAndMax();
		imp.updateAndRepaintWindow();
		imp.unlock();
	}
	
	private Thread[] newThreadArray() {
		int n_cpus = Runtime.getRuntime().availableProcessors();
		if (n_cpus > thread_num_) n_cpus = thread_num_;
		if (n_cpus <= 0) n_cpus = 1;
		return new Thread[n_cpus];
	}
	
	public static void startAndJoin(Thread[] threads)
	{
		for (int ithread = 0; ithread < threads.length; ++ithread)
		{
			threads[ithread].setPriority(Thread.NORM_PRIORITY);
			threads[ithread].start();
		}
		
		try
		{   
			for (int ithread = 0; ithread < threads.length; ++ithread)
			threads[ithread].join();
		} catch (InterruptedException ie)
		{
			throw new RuntimeException(ie);
		}
	}
}

