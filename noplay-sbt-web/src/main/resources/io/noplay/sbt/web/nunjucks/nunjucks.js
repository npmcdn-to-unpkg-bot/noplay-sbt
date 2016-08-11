/*global process, require */

(function () {

	"use strict";

	function relativize(path) {
		return path.startsWith('/') ? path.substring(1) : path;
	}

	var args = process.argv,
		fs = require("fs"),
		mkdirp = require("mkdirp"),
		path = require("path"),
		nunjucks = require("nunjucks");

	var SOURCE_FILE_MAPPINGS_ARG = 2;
	var TARGET_ARG = 3;
	var OPTIONS_ARG = 4;

	var sourceFileMappings = JSON.parse(args[SOURCE_FILE_MAPPINGS_ARG]);
	var target = args[TARGET_ARG];
	var options = JSON.parse(args[OPTIONS_ARG]);
	var baseUrl = options.baseUrl && relativize(options.baseUrl) || '';

	var sourcesToProcess = sourceFileMappings.length;
	var results = [];
	var errors = [];

	function processingDone() {
		if (--sourcesToProcess === 0) {
			console.log("\u0010" + JSON.stringify({results: results, problems: errors}));
		}
	}

	function throwIfErr(e) {
		if (e) throw e;
	}

	sourceFileMappings.forEach(function (sourceFileMapping) {

		var input = sourceFileMapping[0];
		var name = relativize(sourceFileMapping[1].replace(baseUrl, ''));
		var outputFile = sourceFileMapping[1].replace(".njk", ".njk.js");
		var output = path.join(target, outputFile);

		fs.readFile(input, "utf8", function (e, template) {
			throwIfErr(e);

			try {
				options.name = name;
				var precompileTemplate = nunjucks.precompileString(template, options);
				mkdirp(path.dirname(output), function (e) {
					throwIfErr(e);

					fs.writeFile(output, precompileTemplate, "utf8", function (e) {
						throwIfErr(e);

						results.push({
							source: input,
							result: {
								filesRead: [input],
								filesWritten: [output]
							}
						});

						processingDone();
					});
				});

			} catch (error) {
				var templateLines = template.match(/[^\r\n]+/g);
				errors.push({
					message: error.message,
					severity: 'error',
					lineNumber: error.lineno,
					characterOffset: error.colno,
					lineContents: templateLines[error.lineno],
					source: input
				});
				results.push({
					source: input,
					result: null
				});

				processingDone();
			}

		});
	});
})();