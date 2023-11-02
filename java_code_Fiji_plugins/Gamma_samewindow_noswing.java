//************************************************
// Gamma adjustment plugin 
// Written by Hideo Otsuna (HHMI Janelia inst.)
// Aug 2015
// 
//**************************************************

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
//import ij.plugin.*;
import ij.plugin.PlugIn;
import ij.plugin.frame.*; 
import ij.plugin.filter.*;
//import ij.plugin.Macro_Runner.*;
import ij.gui.GenericDialog.*;
import ij.macro.*;

import java.util.concurrent.atomic.AtomicInteger;

public class Gamma_samewindow_noswing implements PlugInFilter {
	//int wList [] = WindowManager.getIDList();
	
	ImagePlus imp, nimp;
	ImageProcessor ip, ip2;
	int bittype=0;
	int sourceRR=0;
	int countsource;
	int count;
	String macro;
	int sliceposition=0;
	boolean ThreeD=false;
	double gamma=0;
	int majorT=0;
	int minorT=0;
	double setvalue=0;
	int [] RR= new int [2];
	int thread_num_=0;
	int defaultgamma=0,curslice=0,Finthread_num_=1;
	
	public int setup (String arg, ImagePlus imp){//
		imp = WindowManager.getCurrentImage();
		//	this.imp = imp;
		
		//		IJ.register (Gamma_samewindow_noswing.class);
		if (IJ.versionLessThan("1.32c")){
			IJ.showMessage("Error", "Please Update ImageJ.");
			return 0;
		}
		int[] wList = WindowManager.getIDList();
		if (wList==null) {
			IJ.error("No images are open.");
			return 0;
		}
		if(imp.getType()!=imp.GRAY8 && imp.getType()!=imp.GRAY16){
			IJ.showMessage("Error", "Plugin requires 8- or 16-bit image");
			return 0;
		}
		
		IJ.log("imp.getType(); "+imp.getType());
		
		
		if(imp.getType()==0){
			bittype=0;
			majorT=3;
			minorT=3;
			setvalue=255;
		}else if(imp.getType()==1){
			bittype=1;
			majorT=3;
			minorT=3;
			setvalue=65535;
		}
		
		curslice=imp.getSlice();
		int bdepth = imp.getBitDepth();
		
		gamma = (double)Prefs.get("gamma.double", 1.3);
		ThreeD = (boolean)Prefs.get("ThreeD.boolean",false);
		
		thread_num_=(int)Prefs.get("thread_numG.int",4);
		
		GenericDialog gd = new GenericDialog("Background thresholding");
		
		gd.addNumericField("Gamma value 0.1~3.0", gamma, 2);
		gd.addCheckbox("3D stack", ThreeD);
		
		gd.addNumericField("CPU number", thread_num_, 2);
		
		gd.showDialog();
		if(gd.wasCanceled()){
			return 0;
		}
		
		gamma = (double)gd.getNextNumber();
		ThreeD = gd.getNextBoolean();
		
		thread_num_ = (int)gd.getNextNumber();
		
		Prefs.set("gamma.double", gamma);
		Prefs.set("ThreeD.boolean", ThreeD);
		if(thread_num_ <= 0) thread_num_ = 1;
		Prefs.set("thread_numG.int", thread_num_);
		
		final double tengamma=gamma*10;
		defaultgamma = (int) tengamma;
		
		Finthread_num_=thread_num_;
		
		this.imp = imp;
		return DOES_ALL;
		
	} //public void run(String arg) {
	

	public void run(ImageProcessor ip){
		
		int FsourceRR=defaultgamma;
		int Fsliceposition=curslice;
		final double Fsetvalue=setvalue;
		boolean FThreeD=ThreeD;
		final ImagePlus Fimp=imp;
		int Fthread_num_=Finthread_num_;
		
	//	IJ.log ("Fsetvalue; "+String.valueOf(setvalue));
		
		imp = WindowManager.getCurrentImage();
		final double Dgamma= (double) FsourceRR/ (double) 10;
		
		//	IJ.log("  Dgamma;"+String.valueOf(Dgamma));
		
		if(FThreeD==true){
			int nslice = Fimp.getNSlices();
			
			IJ.log("nslice; "+nslice);
			
			if(Fthread_num_ <= 0) Fthread_num_ = 1;
			Prefs.set("thread_num.int", Fthread_num_);
			
			for(int iii=1; iii<=nslice; iii++){
				
				final AtomicInteger ai = new AtomicInteger(0);
				final Thread[] threads = newThreadArray();
				final int Fiii=iii;
				final int Fnslice=nslice;
				
				for (int ithread = 0; ithread < threads.length; ithread++) {
					// Concurrently run in as many threads as CPUs
					threads[ithread] = new Thread() {
						
						{ setPriority(Thread.NORM_PRIORITY); }
						
						public void run() {
							ImageProcessor Fip =Fimp.getProcessor();
							ImageStack stack1=null;
							
							if(Fnslice>1){
								stack1 = Fimp.getStack();
								Fip=stack1.getProcessor(Fiii);
							}
							int sumpx = Fip.getPixelCount();
							IJ.showProgress((double)Fiii/(double)Fnslice);
							
							for(int ii=ai.getAndIncrement(); ii<sumpx; ii = ai.getAndIncrement()){
								double pix=Fip.get(ii);
								
								double out= (double) Fsetvalue*Math.pow( (double) pix/ (double) Fsetvalue, (double) 1/ (double)  Dgamma);
								
								if(out>Fsetvalue)
								out= (int) Fsetvalue;
								
								if(out<0)
								out= (int) 0;
								
								Fip.set(ii, (int) out);
								
							}
					}};
				}//	for (int ithread = 0; ithread < threads.length; ithread++) {
				startAndJoin(threads);
			}//	for(int iii=1; iii<=nslice; iii++){
			
			Fimp.unlock();
			Fimp.show();
			Fimp.setSlice(curslice);
			
			Fimp.updateImage();
		}else{//if(ThreeD not rue){
			//	IJ.log("line 186");
			ImageProcessor Fip = imp.getProcessor();
			int sumpx = Fip.getPixelCount();
			for(int ii=0; ii<sumpx; ii++){
				double pix=Fip.get(ii);
				
				double out=setvalue*Math.pow( pix/ setvalue, (double) 1/ (double)  Dgamma);
				
				if(out>setvalue)
				out= (int) setvalue;
				Fip.set(ii, (int) out);
			}
			imp.unlock();
			imp.show();
			imp.updateImage();
		}
		
		
		
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



