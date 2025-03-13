const ts=require("typescript");
const fs=require("fs");
const path=require("path");
const UglifyJS=require("uglify-js");
const crypto=require("crypto");
const postcss=require("postcss");

var cssFiles=["desktop.scss", "mobile.scss", "embed.scss"];

if(process.argv.length<3)
	throw Error("Specify the java project directory as an argument");

var projectDir=process.argv[2];
var files=fs.readdirSync(".", {withFileTypes: true});
var staticFileVersions={};

var resOutputDir=path.join(projectDir, "target/generated-resources/public/res");
if(!fs.existsSync(resOutputDir)){
	fs.mkdirSync(resOutputDir, {recursive: true});
}

// TypeScript

var dirNames=["common_ts"]; // Make sure common.js compiles first so other files can depend on it

for(var file of files){
	if(file.isDirectory() && file.name.substr(-3)=="_ts" && file.name!="common_ts"){
		dirNames.push(file.name);
	}
}

for(var dir of dirNames){
	var config=path.join(dir, "tsconfig.json");
	if(!fs.existsSync(config))
		throw Error(`TypeScript config ${config} does not exist`);
	
	console.log(`Compiling TypeScript from ${dir}`)
	config=compile(config, dir.replace("_ts", ".js"));
	var tsOutput=config.options.outFile;
	var tsMap=tsOutput+".map";
	console.log(`Running UglifyJS on ${tsOutput}`);
	var outName=path.basename(tsOutput);
	var files={};
	files[outName]=fs.readFileSync(tsOutput, "utf-8");
	var minifyResult=UglifyJS.minify(files, {
		sourceMap: {
			content: fs.readFileSync(tsMap, "utf-8"),
			url: outName+".map",
			root: dir
		}
	});

	if(minifyResult.error)
		throw minifyResult.error;

	fs.writeFileSync(path.join(resOutputDir, outName), minifyResult.code);
	fs.writeFileSync(path.join(resOutputDir, outName+".map"), minifyResult.map);
	staticFileVersions[outName]=crypto.createHash("sha1").update(minifyResult.code).digest("hex");

	// Copy TS sources into resources for debugging purposes
	for(var inFile of config.fileNames){
		if(inFile.substr(0, dir.length)!=dir)
			continue;
		var inDir=path.dirname(inFile);
		if(!fs.existsSync(path.join(resOutputDir, inDir))){
			fs.mkdirSync(path.join(resOutputDir, inDir), {recursive: true});
		}
		fs.copyFileSync(inFile, path.join(resOutputDir, inFile));
	}
}

// PostCSS

var postCssConfig=require("./postcss.config.js");
var postCssPromises=[];

for(var cssFile of cssFiles){
	console.log(`Running PostCSS on ${cssFile}`);
	var css=fs.readFileSync(cssFile, "utf-8");
	var outFile=path.join(resOutputDir, cssFile.replace(".scss", ".css"));
	var promise=postcss(postCssConfig.plugins)
		.process(css, {from: cssFile, to: outFile})
		.then(result=>{
			fs.writeFileSync(result.opts.to, result.css);
			staticFileVersions[path.basename(result.opts.to)]=crypto.createHash("sha1").update(result.css).digest("hex");
		});
	postCssPromises.push(promise);
}

Promise.all(postCssPromises).then(results=>{
	fs.writeFileSync(path.join(projectDir, "target/generated-resources/static_file_versions.json"), JSON.stringify(staticFileVersions));
});

// Mostly copied from https://github.com/Microsoft/TypeScript/issues/6387

function reportDiagnostics(diagnostics) { 
	diagnostics.forEach(diagnostic => {
		let message = "Error";
		if (diagnostic.file) {
			let { line, character } = diagnostic.file.getLineAndCharacterOfPosition(diagnostic.start);
			message += ` ${diagnostic.file.fileName} (${line + 1},${character + 1})`;
		}
		message += ": " + ts.flattenDiagnosticMessageText(diagnostic.messageText, '\n');
		console.log(message);
	});
}

function readConfigFile(configFileName) { 
	// Read config file
	const configFileText = fs.readFileSync(configFileName).toString();  

	// Parse JSON, after removing comments. Just fancier JSON.parse
	const result = ts.parseConfigFileTextToJson(configFileName, configFileText);
	const configObject = result.config;
	if (!configObject) {
		reportDiagnostics([result.error]);
		process.exit(1);
	}

	// Extract config infromation
	const configParseResult = ts.parseJsonConfigFileContent(configObject, ts.sys, path.dirname(configFileName));
	if (configParseResult.errors.length > 0) {
		reportDiagnostics(configParseResult.errors);
		process.exit(1);
	}
	return configParseResult;
}


function compile(configFileName, outFileName) {
	// Extract configuration from config file
	let config = readConfigFile(configFileName);
	config.options.outFile=path.join(projectDir, "target", "typescript", outFileName);
	if(outFileName!="common.js"){ // Other files can depend on common.js
		config.fileNames.push(path.join(projectDir, "target/typescript/common.d.ts"));
	}

	// Compile
	let program = ts.createProgram(config.fileNames, config.options);
	let emitResult = program.emit();

	// Report errors
	reportDiagnostics(ts.getPreEmitDiagnostics(program).concat(emitResult.diagnostics));

	// Return code
	if(emitResult.emitSkipped){
		process.exit(1);
	}
	return config;
}