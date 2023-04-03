namespace smithereen.graph{
	export class Utils{
		public static cssColor(rgb:number, alpha:number):string{
			return `rgba(${rgb >> 16}, ${(rgb >> 8) & 0xFF}, ${rgb & 0xFF}, ${alpha})`;
		}

		public static drawRect(ctx:CanvasRenderingContext2D, x:number, y:number, w:number, h:number, fillColor:number=16777215, fillAlpha:number=1, strokeWidth:number=0, strokeColor:number=0, strokeAlpha:number=1){
			ctx.fillStyle=Utils.cssColor(fillColor, fillAlpha);
			ctx.fillRect(x, y, w, h);
			if(strokeWidth){
				ctx.lineWidth=strokeWidth;
				ctx.strokeStyle=Utils.cssColor(strokeColor, strokeAlpha);
				ctx.strokeRect(x+0.5, y+0.5, w-1, h-1);
			}
		}

		public static prepareYGrid(graph:Graph, height:number):YGridLine[]{
			var lines:YGridLine[]=[];
			var minDate=graph.getMinDate();
			var maxDate=graph.getMaxDate();
			if(minDate==Number.MAX_VALUE)
				return lines;
			var minDisplayedDate=minDate+graph.slider.xLeft/(graph.slider.width-1)*(maxDate-minDate);
			var maxDisplayedDate=minDate+graph.slider.xRight/(graph.slider.width-1)*(maxDate-minDate);
			minDisplayedDate=Math.floor(minDisplayedDate*1000)/1000;
			maxDisplayedDate=Math.floor(maxDisplayedDate*1000)/1000;
			var minValue=graph.slScale.minValue<0 ? graph.slScale.minValue : 0;
			var maxValue=graph.getMaxValue(minDisplayedDate, maxDisplayedDate);
			var valueRange=maxValue-minValue;
			var _step=Math.pow(10, Math.floor(Math.LOG10E*Math.log(valueRange))) || 1;
			var step;
			if(valueRange/_step>4){
				step=_step;
			}else if(valueRange/_step>2){
				step=_step*0.5;
			}else{
				step=_step*0.25;
			}
			if(graph.intScale && Math.round(step)!=step){
				step=_step;
			}
			var decimalPlaces=0;
			for(var s=step;s!=Math.floor(s);s*=10){
				decimalPlaces++;
			}
			var firstLineY=Math.floor(minValue/step)*step;
			var lastLineY=Math.ceil(maxValue/step)*step;
			if(firstLineY>lastLineY && minValue==maxValue){
				lines.push({y: height/2, v: minValue});
			}
			for(var curLineY=firstLineY;curLineY<=lastLineY;curLineY+=step){
				var roundedValue=Math.round(curLineY*Math.pow(10, decimalPlaces))/Math.pow(10, decimalPlaces);
				if(firstLineY==lastLineY && curLineY==firstLineY){
					if(curLineY==0)
						lines.push({y: height, v: roundedValue});
					else
						lines.push({y: height/2, v: roundedValue});
				}else{
					lines.push({y: (lastLineY-curLineY)*height/(lastLineY-firstLineY), v: roundedValue});
				}
			}
			return lines;
		}

		private static padStr(s:string):string{
			if(s.length>=2)
				return s;
			return "0"+s;
		}

		public static formatDate(type:string, date:Date):string{
			switch(type){
				case "hour":
					return date.getHours()+":"+Utils.padStr(date.getMinutes().toString());
				case "day_month":
					return lang("date_format_current_year", {day: date.getDate(), month: lang("month_short", {month: date.getMonth()+1}), year: date.getFullYear()});
				case "day_fullMonth":
					return lang("date_format_current_year", {day: date.getDate(), month: lang("month_full", {month: date.getMonth()+1}), year: date.getFullYear()});
				case "month_year":
					return lang("date_format_month_year_short", {month: lang("month_short", {month: date.getMonth()+1}), year: date.getFullYear().toString().slice(-2)});
				case "fullMonth_year":
					return lang("date_format_month_year", {month: lang("month_standalone", {month: date.getMonth()+1}), year: date.getFullYear()});
				case "year":
					return date.getFullYear().toString();
				case "dayMonthYear":
					return lang("date_format_other_year", {day: date.getDate(), month: lang("month_full", {month: date.getMonth()+1}), year: date.getFullYear()});
			}
			return date.toString();
		}

		public static getDateRange(startTimestamp:number, endTimestamp:number, width:number):XGridLine[]{
			if(!isFinite(startTimestamp) || !isFinite(endTimestamp))
				throw new Error();
			var lines:XGridLine[]=[];
			var startDate=new Date(startTimestamp*1000);
			var endDate=new Date(endTimestamp*1000);
			if(startTimestamp==endTimestamp || startTimestamp<0){
				var currentDate=new Date();
				var currentTimestamp=currentDate.getTime()/1000;
				var start=startTimestamp>0 ? startTimestamp : currentTimestamp-currentTimestamp%3600-(currentDate.getHours()-12);
				startTimestamp=start-3600*24*3.5;
				endTimestamp=start+3600*24*3.5;
				startDate=new Date(startTimestamp*1000);
				endDate=new Date(endTimestamp*1000);
			}
			var scaleFactor=width/(endTimestamp-startTimestamp);
			var rangeInHours=Math.ceil((endTimestamp-startTimestamp)/3600);
			var rangeInDays=Math.ceil(rangeInHours/24);
			var dateFormatType:string;
			if(width/60>rangeInHours*2/24){
				dateFormatType="hour";
				var date=new Date(startTimestamp*1000);
				date.setMinutes(30);
				date.setSeconds(0);
				date.setMilliseconds(0);
				var step=Math.ceil(rangeInHours/width*60);
				if(step>4){
					if(step>8)
						step=12;
					else if(step>6)
						step=8;
					else
						step=6;
				}
				if(date<startDate)
					date.setTime(date.getTime()+3600000);
				var extraHours=date.getHours()%step;
				while(date<endDate || step==1){
					if(step==1){
						var d=new Date(date.getTime());
						d.setMinutes(0);
						lines.push({x: (d.getTime()/1000-startTimestamp)*scaleFactor, p: 3});
						if(date>=endDate)
							break;
					}
					for(var i=extraHours;i<step;i++){
						lines.push({x: (date.getTime()/1000-startTimestamp)*scaleFactor, v: i==0 ? Utils.formatDate(dateFormatType, date) : null, p: (step==1 ? 0 : (i==0 ? 3 : 1))});
						date.setTime(date.getTime()+3600000);
					}
					extraHours=0;
				}
				return lines;
			}
			if(width/60>rangeInDays*2/30 && width/15>rangeInDays){
				dateFormatType="day_month";
				var date=new Date(startTimestamp*1000);
				date.setHours(12);
				date.setMinutes(0);
				date.setSeconds(0);
				var _step=rangeInDays/width*60;
				var step=Math.ceil(_step);
				if(step==5){
					step=6;
				}
				if(step/_step>1.5){
					dateFormatType="day_fullMonth";
				}
				if(date<startDate)
					date.setDate(date.getDate()+1);
				var extraDays=Math.floor(date.getTime()/1000/87600)%step;
				while(date<endDate || step==1){
					if(step==1){
						var d=new Date(date.getTime());
						d.setHours(0);
						lines.push({x: (d.getTime()/1000-startTimestamp)*scaleFactor, p: 3});
						if(date>=endDate)
							break;
					}
					for(var i=extraDays;i<step;i++){
						lines.push({x: (date.getTime()/1000-startTimestamp)*scaleFactor, v: i==0 ? Utils.formatDate(dateFormatType, date) : null, p: (step==1 ? 0 : (i==0 ? 3 : 1))});
						date.setDate(date.getDate()+1);
					}
					extraDays=0;
				}
				return lines;
			}
			if(width/60>rangeInDays*2/(30*12) && width/15>rangeInDays/30){
				dateFormatType="month_year";
				var date=new Date(startTimestamp*1000);
				date.setDate(15);
				date.setHours(0);
				date.setMinutes(0);
				var _step=(Math.floor(rangeInDays/30) || 1)/width*60;
				var step=Math.ceil(_step);
				if(step==5)
					step=6;
				if(step/_step>1.5)
					dateFormatType="fullMonth_year";
				if(date<startDate)
					date.setMonth(date.getMonth()+1);
				var extraMonths=date.getMonth()%step;
				while(date<endDate || step==1){
					if(step==1){
						var d=new Date(date.getTime());
						d.setDate(1);
						lines.push({x: (d.getTime()/1000-startTimestamp)*scaleFactor, p: 3});
						if(date>=endDate)
							break;
					}
					for(var i=extraMonths;i<step;i++){
						lines.push({x: (date.getTime()/1000-startTimestamp)*scaleFactor, v: i==0 ? Utils.formatDate(dateFormatType, date) : null, p: (step==1 ? 0 : (i==0 ? 3 : 1))});
						date.setMonth(date.getMonth()+1);
					}
					extraMonths=0;
				}
				return lines;
			}
			dateFormatType="year";
			var date=new Date(startTimestamp*1000);
			date.setMonth(7);
			date.setDate(1);
			date.setHours(0);
			var step=Math.ceil((Math.floor(rangeInDays/365) || 1)/width*60);
			if(date<startDate)
				date.setFullYear(date.getFullYear()+1);
			var extraYears=date.getFullYear()%step;
			while(date<endDate || step==1){
				if(step==1){
					var d=new Date(date.getTime());
					d.setMonth(1);
					lines.push({x: (d.getTime()/1000-startTimestamp)*scaleFactor, p: 3});
					if(date>=endDate)
						break;
				}
				for(var i=extraYears;i<step;i++){
					lines.push({x: (date.getTime()/1000-startTimestamp)*scaleFactor, v: i==0 ? Utils.formatDate(dateFormatType, date) : null, p: (step==1 ? 0 : (i==0 ? 3 : 1))});
					date.setFullYear(date.getFullYear()+1);
				}
				extraYears=0;
			}
			return lines;
		}

		public static formatNumber(n:number):string{
			return n.toLocaleString();
		}

		public static getFirstPoint(startDate:number, category:GraphCategory, minOffset:number){
			if(category.points.length==0 || !category.active)
				return -1;
			for(var i=0;i<category.points.length;i++){
				if(category.points[i].x>=startDate){
					if(i<minOffset)
						return 0;
					return i-minOffset;
				}
			}
			return category.points.length-1;
		}

		public static getLastPoint(endDate:number, category:GraphCategory, minOffset:number){
			if(category.points.length==0 || !category.active)
				return -1;
			for(var i=category.points.length-1;i>=0;i--){
				if(category.points[i].x<=endDate){
					if(i>category.points.length-1-minOffset){
						return category.points.length-1;
					}
					return i+minOffset;
				}
			}
			return 0;
		}
	}

	interface YGridLine{
		y:number;
		v:number;
	}

	interface XGridLine{
		x:number;
		p:number;
		v?:string;
	}
}
