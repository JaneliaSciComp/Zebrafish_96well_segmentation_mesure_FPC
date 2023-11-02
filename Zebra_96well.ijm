//************************************************
// Zebrafish auto 2D segmentation & auto FPC
// Written by Hideo Otsuna (HHMI Janelia inst.)
// Oct 2023
// 
//**************************************************

outline=1;
measureFPC="Automatic";//"Manual"
dirOK=false;
dir="";
SegSensitivity=6;
CSVsave="New CSV";

filepath0=getDirectory("temp");//C:\Users\??\AppData\Local\Temp\...C:\DOCUME~1\ADMINI~1\LOCALS~1\Temp\
filepath=filepath0+"Zebrafish_batch.txt";

print("filepath; "+filepath);

LF=10; TAB=9; swi=0; swi2=0; testline=0;
exi=File.exists(filepath);
List.clear();

if(exi==1){
	s1 = File.openAsRawString(filepath);
	swin=0;
	swi2n=-1;
	
	n = lengthOf(s1);
	String.resetBuffer;
	for (testnum=0; testnum<n; testnum++) {
		enter = charCodeAt(s1, testnum);
		
		if(enter==10)
		testline=testline+1;//line number
	}
	
	String.resetBuffer;
	for (si=0; si<n; si++) {
		c = charCodeAt(s1, si);
		
		if(c==10){
			swi=swi+1;
			swin=swin+1;
			swi2n=swi-1;
		}
		
		if(swi==swin){
			if(swi2==swi2n){
				String.resetBuffer;
				swi2=swi;
			}
			if (c>=32 && c<=127)
			String.append(fromCharCode(c));
		}
		if(swi==0){
			outline = String.buffer;
		}else if(swi==1 && swi<=testline){
			measureFPC = String.buffer;
		}else if(swi==2 && swi<=testline){
			dir = String.buffer;
		}else if(swi==3 && swi<=testline){
			dirOK = String.buffer;
		}else if(swi==4 && swi<=testline){
			SegSensitivity = String.buffer;
		}else if(swi==5 && swi<=testline){
			CSVsave = String.buffer;
		}
	}
}

Dialog.create("zebrafish FPC measurement");
Dialog.addCheckbox("Image Dir"+dir+" OK?",dirOK);

item5=newArray("Automatic", "Manual","None & segmentation only");
Dialog.addRadioButtonGroup("FPC measuring method: ", item5, 1, 2, measureFPC); 
item4=newArray("New CSV", "Append to existing CSV");
Dialog.addRadioButtonGroup("Total CSV save method: ", item4, 1, 2, CSVsave); 

Dialog.addSlider("Segmentation sensitivity; lower is tight, higher is loose, default 6", 2, 10, SegSensitivity)




Dialog.addCheckbox("Export outline image",outline);
Dialog.show();

dirOK=Dialog.getCheckbox();
measureFPC = Dialog.getRadioButton();
CSVsave= Dialog.getRadioButton();
SegSensitivity = Dialog.getNumber();

outline=Dialog.getCheckbox();

if(dirOK!=true){
	dir=getDirectory("Choose a Directory for images");
}

list=getFileList(dir);
Array.sort(list);

print("SegSensitivity; "+SegSensitivity);

File.saveString(outline+"\n"+measureFPC+"\n"+dir+"\n"+dirOK+"\n"+SegSensitivity+"\n"+CSVsave, filepath);

dirnolast=substring(dir,0,lengthOf(dir)-1);

filepathindex=lastIndexOf(dirnolast,File.separator);
dirname=substring(dirnolast,filepathindex+1,lengthOf(dirnolast));
savedir=substring(dirnolast,0,filepathindex+1);

setBatchMode(true);


if(CSVsave=="New CSV"){
	totalString="Sample,Sum area,Ave area,Sum bri.,Ave bri.,Num FPC"+"\n";
	File.saveString(totalString, savedir+dirname+".csv");
}else{
	totalString=File.openAsString(savedir+dirname+".csv");
	
	//print("totalString; "+totalString);

		totalStringLines=split(totalString,"\n");
		newTotalString="";
		
		for(iline=0; iline<totalStringLines.length; iline++){
			
			totalaveIndex=indexOf (totalStringLines[iline],"Total ave;");
			
			if(totalaveIndex==-1)
			newTotalString=newTotalString+totalStringLines[iline]+"\n";
		}
		//print("newTotalString; "+newTotalString);
		//aaa
	File.saveString(newTotalString, savedir+dirname+".csv");
}




for(iii=0; iii<list.length; iii++){
	showProgress(iii/list.length);
	
	if(endsWith(list[iii],"tif")){
		
		dotindex=lastIndexOf(list[iii],".");
		if(dotindex!=-1)
		truename=substring(list[iii],0,dotindex);
		
		open(dir+list[iii]);
		
		getDimensions(width, height, channels, slices, frames);
		
		//print(iii+1+" width; "+width);
		
		minsizeratio=width/1024;
		
		minsize=35000*minsizeratio;
		
		oriimg=getTitle();
		bitd=bitDepth();
		
		run("Duplicate...", "title=round");
		run("Enhance Contrast", "saturated=0.2");
		setAutoThreshold("MinError dark no-reset");
		setOption("BlackBackground", true);
		run("Convert to Mask");
		run("Fill Holes");
		run("Subtract Background...", "rolling=35");
		
		setThreshold(3, 255);
		run("Convert to Mask");
		
		//setBatchMode(false);
		//updateDisplay();
		//aaa
		
		//	if(width>1800)
		run("Maximum...", "radius="+round(2*minsizeratio)+"");
		
		if(bitd==16)
		mask8to16();
		
		print("");
		selectWindow(oriimg);
		dupAndSubBackArray=newArray("false",0);
		dupAndSubBack ("FishMask",bitd,dupAndSubBackArray);
		
		maxsize=255;
		if(bitd==16)
		maxsize=65535;
		
		if(dupAndSubBackArray[0]==true)
		SegSensitivity=SegSensitivity+3;
		
		thresholdDesition_maskArray= newArray(0,maxsize,10,SegSensitivity);
		thresholdDesition(thresholdDesition_maskArray);
		
		if(dupAndSubBackArray[0]==true)
		SegSensitivity=SegSensitivity-3;
		
		bestthreValue=thresholdDesition_maskArray[0];
		
		print(list[iii]+" bestthreValue; "+bestthreValue);
		setThreshold(bestthreValue, maxsize);
		run("Convert to Mask");
		
		run("Size based Noise elimination", "ignore=229 less=3");
		run("Close-");
		run("Fill Holes");
		run("Maximum...", "radius=15");
		run("Minimum...", "radius=15");
		run("Maximum...", "radius=3");
		run("Fill Holes");
		
		//		setBatchMode(false);
		//	updateDisplay();
		//		aaa
		
		run("Set Measurements...", "area centroid center perimeter fit shape stack redirect=None decimal=2");
		run("Analyze Particles...", "size="+minsize+"-Infinity show=[Count Masks] display clear");
		run("Grays");
		fishMask=getTitle();
		updateResults();
		
		if(getValue("results.count")>0){
			
			
			maxAreaa=0;
			for(iarea=0; iarea<getValue("results.count"); iarea++){
				Areaval=getResult("Area", iarea);
				
				if(Areaval>maxAreaa){
					maxAreaa=Areaval;
					ARvalue=getResult("AR", iarea);
					bestID=iarea+1;
				}
			}
			
			if(getValue("results.count")>1)
			deleteSmall(bestID);
			
			
			print(iii+1+"maxAreaa; "+maxAreaa+"  ARvalue; "+ARvalue+"  result Num; "+getValue("results.count"));
			if(ARvalue<4){
				
				while(isOpen("FishMask")){
					selectWindow("FishMask");
					close();
				}
				
				while(isOpen(fishMask)){
					selectWindow(fishMask);
					close();
				}
				
				selectWindow(oriimg);
				dupAndSubBack ("FishMask",bitd,dupAndSubBackArray);
				
				testThre=bestthreValue;
				selectWindow("FishMask");
				
				loopnum=0;
				while (ARvalue<4){
					selectWindow("FishMask");
					run("Duplicate...", "title=FishMaskTest");
					
					testThre=testThre+30;
					setThreshold(testThre, maxsize);
					run("Convert to Mask");
					
					run("Size based Noise elimination", "ignore=229 less=3");
					run("Close-");
					run("Fill Holes");
					run("Maximum...", "radius=15");
					run("Minimum...", "radius=12");
					run("Fill Holes");
					
					//			setBatchMode(false);
					//				updateDisplay();
					//				aaa
					
					run("Analyze Particles...", "size="+minsize+"-Infinity show=[Count Masks] display clear");
					fishMask=getTitle();
					run("Grays");
					
					updateResults();
					
					maxAreaa=0; bestID=0;
					for(iarea=0; iarea<getValue("results.count"); iarea++){
						Areaval=getResult("Area", iarea);
						
						if(Areaval>maxAreaa){
							maxAreaa=Areaval;
							ARvalue=getResult("AR", iarea);
							bestID=iarea+1;
						}
					}
					
					//		print(loopnum+"  testThre; "+testThre+"  ARvalue; "+ARvalue);
					if(ARvalue<4 && maxAreaa>minsize){
						close();
						
						selectWindow("FishMaskTest");
						close();
					}else{
						bestthreValue=testThre;
						print(iii+1+"bestthreValue2; "+bestthreValue+"  maxAreaa; "+maxAreaa+"  ARvalue; "+ARvalue+"  result Num; "+getValue("results.count"));
						if(getValue("results.count")>1)
						deleteSmall(bestID);
						break();
					}
					loopnum=loopnum+1;
					
				}
			}
			
			if(ARvalue>=4){
				
				selectWindow(fishMask);
				if(bitd==16)
				mask8to16();
				
				selectWindow("FishMask");
				close();
				
				selectWindow("round");
				close();
				
				imageCalculator("AND", oriimg,fishMask);
				
				if(measureFPC=="Automatic"){
					selectWindow(oriimg);
					run("Hyperstack to Stack");
					run("Duplicate...", "title=FPC");
					
					thresholdDesition_maskArray= newArray(0,maxsize,100,SegSensitivity);
					thresholdDesition(thresholdDesition_maskArray);
					
					bestthreValue=thresholdDesition_maskArray[0];
					print("bestthreValue2; "+bestthreValue);
					
					run("Local contrast thresholding", "scan=15 noise=1px cpu=6 0-200=1.200 <200-600=1.200 <600-1200=1.050 >1200=1.020 min="+bestthreValue+" max=65535");
					run("8-bit");
					
					FPCbinary=getTitle();
					
					//		setAutoThreshold("Default dark no-reset");
					//		getThreshold(lower, upper);
					//		setThreshold(lower, upper);
					//		run("Convert to Mask");
					//		run("Invert");
					
					run("Set Measurements...", "area centroid perimeter shape redirect=None decimal=2");
					run("Analyze Particles...", "size=2-Infinity show=[Count Masks] display clear");
					
					indexmask=getTitle();
					updateResults();
					
					newImage("measure_slice", "16-bit black", width*2, height, 1);
					
					selectImage(indexmask);
					run("Select All");
					run("Copy");
					selectImage("measure_slice");
					makeRectangle(0, 0, width, height);
					run("Paste");
					
					selectImage(oriimg);
					run("Select All");
					run("Copy");
					selectImage("measure_slice");
					makeRectangle(width, 0, width, height);
					run("Paste");
					
					a=getTime();
					indexArray=newArray(getValue("results.count")+1);
					
					sumbri=0;
					
					for(ix=0; ix<width; ix++){
						for(iy=0; iy<height; iy++){
							
							if(getPixel(ix, iy)>0)
							indexArray[getPixel(ix, iy)]=indexArray[getPixel(ix, iy)]+getPixel(ix+width, iy);
							
						}
					}
					
					sumbri=0; 	sumarea=0; 
					for(indexval=1; indexval<=getValue("results.count"); indexval++){
						
						areacount=getResult("Area", indexval-1);
						avebri=indexArray[indexval]/areacount;
						
						sumbri=sumbri+avebri;
						sumarea=sumarea+areacount;
						setResult("Ave bri.", indexval-1, round(avebri));
					}
					//setResult("Index Area.", indexval-1, count);
					b=getTime();
					
					print((b-a)/1000+" sec for index scan");
					updateResults();
					close();
					
					ResultNumber=round(getValue("results.count"));
					
					addstring=truename+","+d2s(sumarea,0)+","+d2s(round(sumarea/ResultNumber),0)+","+d2s(round(sumbri),0)+","+d2s(round(sumbri/ResultNumber),0)+","+d2s(ResultNumber,0);
					//		print(sumarea+"  "+round(sumarea/ResultNumber)+"    "+addstring);
					
					//			totalString=totalString+addstring;
					saveAs("Results", dir+truename+"_Area_"+sumarea+"_Bri_"+round(sumbri)+"_FPC.csv");
					
					selectImage(FPCbinary);
					
					//		setBatchMode(false);
					//		updateDisplay();
					//		aaa
					
					outlinecreation (2,"FPC",bitd,true,minsizeratio);
					
					saveAs("PNG", ""+dir+truename+"FPC_.png");
					close();
				}else if(measureFPC=="Manual"){
					
					y = getNumber("Threshold", 0);
					setThreshold(0, y);
					run("Convert to Mask");
					run("Invert");
					run("Set Measurements...", "area redirect=None decimal=0");
					run("Analyze Particles...", "size=0-Infinity circularity=0.00-1.00 show=Nothing summarize");
					saveAs("PNG", ""+dir+truename+"FPC_.png");
				}
				
				if(outline){
					
					//			setBatchMode(false);
					//				updateDisplay();
					//					aaa
					
					selectWindow(fishMask);
					outlinecreation (minsize,oriimg,bitd,false,minsizeratio);
					
				}//		if(outline){
				
				run("Hyperstack to Stack");
				saveAs("PNG", ""+dir+truename+".png");
			}	
		}else{//if(getValue("results.count")>0){
			print(oriimg+" the object is too small. Set sensitivity value higher");
		}
		run("Close All");
		
		File.append(addstring, savedir+dirname+".csv");
	}
}

totalString = File.openAsString(savedir+dirname+".csv");
//print("totalString; "+totalString);
lines=split(totalString,"\n");

totalsumArea=0; totalsumAveArea=0; totalsumBri=0; totalsumAveBri=0; totalResNum=0;
resultnumber=lines.length-1;

//print("resultnumber; "+resultnumber);

for(iline=1; iline<=resultnumber; iline++){
	//print("iline; "+iline);
	lineContents = split(lines[iline],",");
	
	totalsumArea = totalsumArea+round(lineContents[1]);
	totalsumAveArea = totalsumAveArea+round(lineContents[2]);
	
	totalsumBri = totalsumBri+round(lineContents[3]);
	totalsumAveBri = totalsumAveBri+round(lineContents[4]);
	totalResNum = totalResNum+round(lineContents[5]);
}


//totalString=totalString+"Total ave; ,"+d2s(round(totalsumArea/resultnumber),0)+","+d2s(round(totalsumAveArea/resultnumber),0)+","+d2s(round(totalsumBri/resultnumber),0)+","+d2s(round(totalsumAveBri/resultnumber),0)+","+d2s(round(totalResNum/resultnumber),0)+"";
appendSTR="Total ave; ,"+d2s(round(totalsumArea/resultnumber),0)+","+d2s(round(totalsumAveArea/resultnumber),0)+","+d2s(round(totalsumBri/resultnumber),0)+","+d2s(round(totalsumAveBri/resultnumber),0)+","+d2s(round(totalResNum/resultnumber),0)+"";

//File.saveString(totalString, savedir+dirname+".csv");

File.append(appendSTR, savedir+dirname+".csv");


function outlinecreation (minsize,oriimgF,bitd,addnum,minsizeratio){
	rename("BinaryMask");
	run("8-bit");
	run("Set Measurements...", "area centroid center perimeter fit shape stack redirect=None decimal=2");
	run("Analyze Particles...", "size="+minsize+"-Infinity show=[Bare Outlines] display clear");
	run("Grays");
	run("Invert LUT");
	run("RGB Color");
	run("Split Channels");
	
	if(addnum==true){
		w=getWidth();
		h=getHeight();
		newImage("Number", "8-bit black", w, h, 1);
		setForegroundColor(255, 255, 255);
		setFont("SansSerif", round(12*minsizeratio), " antialiased");
		
		for(inum=0; inum<getValue("results.count"); inum++){
			drawString(inum+1, getResult("X", inum)+round(5*minsizeratio), getResult("Y", inum)+round(8*minsizeratio));
		}
		selectWindow(oriimgF);
		if(bitd==16)
		run("8-bit");
		run("Merge Channels...", "c1=[Number] c2=[Drawing of BinaryMask (green)] c3=[Number] c4="+oriimgF+"");
	}else{
		selectWindow(oriimgF);
		if(bitd==16)
		run("8-bit");
		
		run("Merge Channels...", "c2=[Drawing of BinaryMask (green)] c4="+oriimgF+"");
		
		//	setBatchMode(false);
		//	updateDisplay();
		//	aaa
	}
	
}


function dupAndSubBack (DupName,bitd,dupAndSubBackArray){
	run("Duplicate...", "title="+DupName+"");
	run("Gamma samewindow noswing", "gamma=1.80 cpu=1");
	run("Enhance Contrast", "saturated=0.2");
	getMinAndMax(min, max);
	
	if(bitd==16 && max>40000){
		run("Gamma samewindow noswing", "gamma=1.40 cpu=1");
		run("Enhance Contrast", "saturated=1");
		getMinAndMax(min, max);
		print("The image was too dim.");
		dupAndSubBackArray[0]=true;
	}
	if(bitd==8 && max>175){
		run("Enhance Contrast", "saturated=1");
		getMinAndMax(min, max);
	}
	
	setMinAndMax(0, max);
	print("Adjusted max; "+max);
	run("Apply LUT");
	run("Gaussian Blur...", "sigma=1");
	run("Subtract Background...", "rolling=35");
	imageCalculator("Subtract", DupName,"round");
}


function deleteSmall (bestID){
	
	for(ix=0; ix<getWidth; ix++){
		for(iy=0; iy<getHeight; iy++){
			pix=getPixel(ix, iy);
			
			if(pix!=bestID)
			setPixel(ix, iy, 0);
		}
	}
}

function mask8to16 (){
	run("16-bit");
	setMinAndMax(0, 1);
	run("Apply LUT");
}

function thresholdDesition (thresholdDesition_maskArray){
	
	maxsize=thresholdDesition_maskArray[1];
	iskip=thresholdDesition_maskArray[2];
	SegSensitivity=thresholdDesition_maskArray[3];
	
	histo=newArray(maxsize+1);
	maxpx=0;
	for(x=0; x<getWidth; x++){
		for(y=0; y<getHeight(); y++){
			pix=getPixel(x, y);
			
			if(pix>0)
			histo[pix]=histo[pix]+1;
			
			if(maxpx<pix)
			maxpx=pix;
		}
	}
	STDarray=newArray(maxpx);
	bestthreValue=0;
	
	sdtmax=0; sdtmaxi=0;
	
	incrinum=10;
	if(iskip<30)
	incrinum=1;
	
	for(i=0; i<maxpx-iskip; i++incrinum){
		
		sum=0;
		
		for(ihisto=i; ihisto<i+iskip; ihisto++){
			sum = sum+(histo[ihisto]-histo[ihisto+1])*(histo[ihisto]-histo[ihisto+1]);
		}
		STDarray[i+1]=sqrt((sum)/iskip);
		
		//	if(STDarray[i+1]<10)
		//		print(i+"_"+STDarray[i+1]);
		
		if(sdtmax<STDarray[i+1]){
			sdtmax=STDarray[i+1];
			sdtmaxi=i+1;
			//	print(i+"_"+STDarray[i+1]);
		}
		
		if(STDarray[i+1]<SegSensitivity && bestthreValue==0 && STDarray[i+1]>0)
		bestthreValue=i+round(iskip/2);
	}
	
	
	STDarray=newArray(maxpx);
	continuous=0;
	if(iskip==100){
		print("sdtmaxi; "+sdtmaxi);
		incri=round(iskip/10);
		for(iscan=sdtmaxi; iscan<maxsize-iskip; iscan+=incri){
			sum=0;
			for(ihisto=iscan; ihisto<iscan+iskip; ihisto++){
				sum = sum+(histo[ihisto]-histo[ihisto+1])*(histo[ihisto]-histo[ihisto+1]);
			}
			STDarray[iscan+1]=sqrt((sum)/iskip);
			
			if(STDarray[iscan+1]>0){
				if(STDarray[iscan+1]<1 )
				continuous=continuous+1;
				else
				continuous=0;
			}
			
			//		print(iscan+"  "+STDarray[iscan+1]);
			
			if(continuous>50){
				bestthreValue=iscan+round(iskip/2);
				break();
			}
		}
	}//	if(iskip==100){
	thresholdDesition_maskArray[0]=bestthreValue;
}

"Done"