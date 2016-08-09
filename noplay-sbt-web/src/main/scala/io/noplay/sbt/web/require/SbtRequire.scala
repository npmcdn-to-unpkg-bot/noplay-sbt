/**
 * Copyright © 2009-2016 Hydra Technologies, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.noplay.sbt.web.require

import java.io.File
import java.nio.charset.Charset

import com.typesafe.sbt.jse.SbtJsEngine.autoImport._
import com.typesafe.sbt.jse.SbtJsTask
import com.typesafe.sbt.jse.SbtJsTask.autoImport._
import com.typesafe.sbt.web.SbtWeb
import com.typesafe.sbt.web.SbtWeb.autoImport.WebKeys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.web.js.{JS, JavaScript}
import com.typesafe.sbt.web.pipeline.Pipeline
import io.alphard.sbt.SbtNpm
import io.alphard.sbt.SbtNpm.autoImport._
import io.noplay.sbt.web.SbtWebIndex
import io.noplay.sbt.web.SbtWebIndex.autoImport._
import sbt.Keys._
import sbt._

object SbtRequire
  extends AutoPlugin {

  override val requires = SbtNpm && SbtWebIndex && SbtJsTask

  object autoImport {

    type RequireId = String
    type RequirePath = String

    object RequirePath {
      def minify(path: String, minified: Boolean = false) = path + (if (minified) ".min" else "")
      def filename(path: String, extension: String = "js"): String = path + "." + extension
      def relativize(path: String): String = if (path.startsWith("/")) path.substring(1) else path
    }

    type RequirePaths = Seq[(RequireId, RequirePath)]
    type RequireBundles = Seq[(RequireId, Seq[RequirePath])]
    type RequireShim = Seq[(String, RequireShimConfig)]

    final case class RequireShimConfig(deps: Seq[RequireId] = Nil, exports: Option[String] = None, init: Option[JavaScript] = None)

    type RequireMap = Seq[(String, Map[RequireId, RequireId])]
    type RequireConfig = Seq[(RequireId, JS[_])]

    final case class RequirePackage(name: String, main: String)

    type RequirePackages = Seq[(RequireId, RequirePackage)]

    sealed trait RequireOptimizer
    object RequireOptimizer {
      final case class Uglify(config: JS.Object = JS.Object()) extends RequireOptimizer
      final case class Uglify2(config: JS.Object = JS.Object()) extends RequireOptimizer
      final case class Closure(config: JS.Object = JS.Object()) extends RequireOptimizer
    }

    type RequireNormalizeDefines = RequireNormalizeDefines.Value
    object RequireNormalizeDefines extends Enumeration {
      val All, Skip = Value
    }

    trait RequireOptimizeCss
    object RequireOptimizeCss {
      case object None extends RequireOptimizeCss
      final case class Standard(keepLines: Boolean = false, keepComments: Boolean = false,
                                keepWhitespace: Boolean = false) extends RequireOptimizeCss
    }

    type RequireLogLevel = RequireLogLevel.Value
    object RequireLogLevel extends Enumeration {
      val Trace, Info, Warn, Error, Silent = Value
    }

    type RequireRawText = Seq[(RequireId, JavaScript)]

    val requireVersion = settingKey[String]("The require js version")
    val requirePath = settingKey[String]("The web jars require js path")
    val requireMinified = settingKey[Boolean]("If true the minified versions of modules in paths are used")
    val requireCDN = settingKey[Boolean]("If true the CDN versions of modules in paths are used")
    val requireOptimized = settingKey[Boolean]("If true an r.js optimization state is added to the pipeline")

    //////////
    // MAIN //
    //////////

    // http://requirejs.org/docs/api.html#config

    val requireMainIncludeFilter = settingKey[FileFilter]("The main include filter generated from paths")
    val requireMainExcludeFilter = settingKey[FileFilter]("The main exclude filter generated from paths")
    val requireMainDirectory = settingKey[File]("The main file directory")
    val requireMainConfigBaseUrl = settingKey[Option[String]]("The root path to use for all module lookup")
    val requireMainConfigPaths = settingKey[RequirePaths](
      "The path mappings for module names not found directly under baseUrl"
    )
    val requireMainConfigMinifiedPaths = settingKey[RequirePaths](
      "The minified path mappings for module names not found directly under baseUrl"
    )
    val requireMainConfigCDNPaths = settingKey[RequirePaths](
      "The CDN path mappings for module names not found directly under baseUrl"
    )
    val requireMainConfigBundles = settingKey[RequireBundles](
      "The bundles allow configuring multiple module IDs to be found in another script"
    )
    val requireMainConfigShim = settingKey[RequireShim](
      """Configure the dependencies, exports, and custom initialization for older,
        |traditional "browser globals" scripts that do not use define()
        |to declare the dependencies and set a module value.
      """.stripMargin
    )
    val requireMainConfigMap = settingKey[RequireMap](
      """For the given module prefix, instead of loading the module
        |with the given ID, substitute a different module ID
      """.stripMargin
    )
    val requireMainConfigConfig = settingKey[RequireConfig](
      """There is a common need to pass configuration info to a module.
        |That configuration info is usually known as part of the application,
        |and there needs to be a way to pass that down to a module.
      """.stripMargin)
    val requireMainConfigPackages = settingKey[RequirePackages](
      """There is a common need to pass configuration info to a module.
        |That configuration info is usually known as part of the application,
        |and there needs to be a way to pass that down to a module.
      """.stripMargin)
    val requireMainConfigNodeIdCompat = settingKey[Boolean](
      """Node treats module ID example.js and example the same. By default these are two different IDs in RequireJS.
        |If you end up using modules installed from npm, then you may need
        |to set this config value to true to avoid resolution issues.
      """.stripMargin
    )
    val requireMainConfigWaitSeconds = settingKey[Int](
      """The number of seconds to wait before giving up on loading a script. Setting it to 0 disables the timeout.
        |The default is 7 seconds.
      """.stripMargin
    )
    val requireMainConfigContext = settingKey[Option[String]](
      """A name to give to a loading context. This allows require.js to load multiple versions of modules in a page,
        |as long as each top-level require call specifies a unique context string. To use it correctly,
        |see the Multiversion Support section.
      """.stripMargin
    )
    val requireMainConfigDeps = settingKey[Seq[RequireId]](
      """An array of dependencies to load.
        |Useful when require is defined as a config object before require.js is loaded,
        |and you want to specify dependencies to load as soon as require() is defined.
      """.stripMargin)
    val requireMainConfigCallback = settingKey[Option[JavaScript]](
      """A function to execute after deps have been loaded.
        |Useful when require is defined as a config object before require.js is loaded,
        |and you want to specify a function to require after the configuration's deps array has been loaded.
      """.stripMargin)
    val requireMainConfigEnforceDefine = settingKey[Boolean](
      """If set to true, an error will be thrown if a script loads that does not call define()
        |or have a shim exports string value that can be checked.""".stripMargin
    )
    val requireMainConfigXhtml = settingKey[Boolean](
      """If set to true, document.createElementNS() will be used to create script elements.
      """.stripMargin
    )
    val requireMainConfigUrlArgs = settingKey[Option[String]](
      "Extra query string arguments appended to URLs that RequireJS uses to fetch resources."
    )
    val requireMainConfigScriptType = settingKey[String](
      """Specify the value for the type="" attribute used for script tags inserted into the document by RequireJS.
        |Default is "text/javascript.
      """.stripMargin
    )
    val requireMainConfigSkipDataMain = settingKey[Boolean](
      """If set to true, skips the data-main attribute scanning done to start module loading.
        | Useful if RequireJS is embedded in a utility library that may interact with other RequireJS library on the page,
        | and the embedded version should not do data-main loading.
      """.stripMargin
    )
    val requireMainConfig = settingKey[JS.Object]("The full configuration object")
    val requireMainModule = settingKey[String]("The main module name")
    val requireMainPath = settingKey[String]("The main file path")
    val requireMainFile = settingKey[File]("The main file")
    val requireMainTemplateFile = settingKey[Option[File]]("The main template file")
    val requireMainGenerator = taskKey[Seq[File]]("Generate the config file")
    val requireMainIndexModule = settingKey[RequireId]("The index module name")

    ///////////
    // BUILD //
    ///////////

    // http://requirejs.org/docs/optimization.html

    val requireBuildIncludeFilter = settingKey[FileFilter]("The require build include filter generated from paths")
    val requireBuildExcludeFilter = settingKey[FileFilter]("The require build exclude filter generated from paths")
    val requireBuildDirectory = settingKey[File]("The require build directory")
    var requireBuildOptimizer = settingKey[RequireOptimizer]("The require optimizer to use: Uglify, Uglify2, Closure")
    val requireBuildConfigAppDir = settingKey[File](
      """The top level directory that contains your app. If this option is used
        |then it assumed your scripts are in a subdirectory under this path.
        |This option is not required. If it is not specified, then baseUrl
        |below is the anchor point for finding things. If this option is specified,
        |then all the files from the app directory will be copied to the dir:
        |output area, and baseUrl will assume to be a relative path under
        |this directory.
      """.stripMargin
    )
    val requireBuildConfigBaseUrl = settingKey[Option[String]](
      """The directory path to save the output. If not specified, then
        |the path will default to be a directory called "build" as a sibling
        |to the build file. All relative paths are relative to the build file.
      """.stripMargin
    )
    val requireBuildConfigMainConfigFiles = settingKey[Seq[File]](
      """By default all the configuration for optimization happens from the command
        |line or by properties in the config file, and configuration that was
        |passed to requirejs as part of the app's runtime "main" JS file is *not*
        |considered. However, if you prefer the "main" JS file configuration
        |to be read for the build so that you do not have to duplicate the values
        |in a separate configuration, set this property to the location of that
        |main JS file. The first requirejs({}), require({}), requirejs.config({}),
        |or require.config({}) call found in that file will be used.
        |As of 2.1.10, mainConfigFile can be an array of values, with the last
        |value's config take precedence over previous values in the array.
      """.stripMargin
    )
    val requireBuildConfigPaths = settingKey[RequirePaths](
      """Set paths for modules. If relative paths, set relative to baseUrl above.
        |If a special value of "empty:" is used for the path value, then that
        |acts like mapping the path to an empty file. It allows the optimizer to
        |resolve the dependency to path, but then does not include it in the output.
        |Useful to map module names that are to resources on a CDN or other
        |http: URL when running in the browser and during an optimization that
        |file should be skipped because it has no dependencies.
        |e.g. if you wish to include `jquery` and `angularjs` from public CDNs,
        |paths: { "jquery": "empty:", "angular": "empty:" }
      """.stripMargin
    )
    val requireBuildConfigMap = settingKey[RequireMap](
      """ Sets up a map of module IDs to other module IDs. For more details, see
        |the http://requirejs.org/docs/api.html#config-map docs.
      """.stripMargin
    )
    val requireBuildConfigPackages = settingKey[RequirePackages](
      """Configure CommonJS packages. See http://requirejs.org/docs/api.html#packages
        |for more information.
      """.stripMargin
    )
    val requireBuildConfigDir = settingKey[File](
      """The directory path to save the output. If not specified, then
        |the path will default to be a directory called "build" as a sibling
        |to the build file. All relative paths are relative to the build file.
      """.stripMargin
    )
    val requireBuildConfigKeepBuildDir = settingKey[Boolean](
      """As of RequireJS 2.0.2, the dir above will be deleted before the
        |build starts again. If you have a big build and are not doing
        |source transforms with onBuildRead/onBuildWrite, then you can
        |set keepBuildDir to true to keep the previous dir. This allows for
        |faster rebuilds, but it could lead to unexpected errors if the
        |built code is transformed in some way.
      """.stripMargin
    )
    val requireBuildConfigShim = settingKey[RequireShim](
      """If shim config is used in the app during runtime, duplicate the config
        |here. Necessary if shim config is used, so that the shim's dependencies
        |are included in the build. Using "mainConfigFile" is a better way to
        |pass this information though, so that it is only listed in one place.
        |However, if mainConfigFile is not an option, the shim config can be
        |inlined in the build config.
      """.stripMargin
    )
    val requireBuildConfigWrapShim = settingKey[Boolean](
      """As of 2.1.11, shimmed dependencies can be wrapped in a define() wrapper
        |to help when intermediate dependencies are AMD have dependencies of their
        |own. The canonical example is a project using Backbone, which depends on
        |jQuery and Underscore. Shimmed dependencies that want Backbone available
        |immediately will not see it in a build, since AMD compatible versions of
        |Backbone will not execute the define() function until dependencies are
        |ready. By wrapping those shimmed dependencies, this can be avoided, but
        |it could introduce other errors if those shimmed dependencies use the
        |global scope in weird ways, so it is not the default behavior to wrap.
        |To use shim wrapping skipModuleInsertion needs to be false.
        |More notes in http://requirejs.org/docs/api.html#config-shim
      """.stripMargin
    )
    val requireBuildConfigLocale = settingKey[Option[String]](
      """Used to inline i18n resources into the built file. If no locale
        |is specified, i18n resources will not be inlined. Only one locale
        |can be inlined for a build. Root bundles referenced by a build layer
        |will be included in a build layer regardless of locale being set.
      """.stripMargin
    )
    val requireBuildConfigSkipDirOptimize = settingKey[Boolean](
      """Introduced in 2.1.2: If using "dir" for an output directory, normally the
        |optimize setting is used to optimize the build bundles (the "modules"
        |section of the config) and any other JS file in the directory. However, if
        |the non-build bundle JS files will not be loaded after a build, you can
        |skip the optimization of those files, to speed up builds. Set this value
        |to true if you want to skip optimizing those other non-build bundle JS
        |files.
      """.stripMargin
    )
    val requireBuildConfigGenerateSourceMaps = settingKey[Boolean](
      """Introduced in 2.1.2 and considered experimental.
        |If the minifier specified in the "optimize" option supports generating
        |source maps for the minified code, then generate them. The source maps
        |generated only translate minified JS to non-minified JS, it does not do
        |anything magical for translating minified JS to transpiled source code.
        |Currently only optimize: "uglify2" is supported when running in node or
        |rhino, and if running in rhino, "closure" with a closure compiler jar
        |build after r1592 (20111114 release).
        |The source files will show up in a browser developer tool that supports
        |source maps as ".js.src" files.
      """.stripMargin
    )
    val requireBuildConfigNormalizeDefines = settingKey[RequireNormalizeDefines](
      """Introduced in 2.1.1: If a full directory optimization ("dir" is used), and
        |optimize is not "none", and skipDirOptimize is false, then normally all JS
        |files in the directory will be minified, and this value is automatically
        |set to "all". For JS files to properly work after a minification, the
        |optimizer will parse for define() calls and insert any dependency arrays
        |that are missing. However, this can be a bit slow if there are many/larger
        |JS files. So this transport normalization is not done (automatically set
        |to "skip") if optimize is set to "none". Cases where you may want to
        |manually set this value:
        |1) Optimizing later: if you plan on minifying the non-build bundle JS files
        |after the optimizer runs (so not as part of running the optimizer), then
        |you should explicitly this value to "all".
        |2) Optimizing, but not dynamically loading later: you want to do a full
        |project optimization, but do not plan on dynamically loading non-build
        |bundle JS files later. In this case, the normalization just slows down
        |builds, so you can explicitly set this value to "skip".
        |Finally, all build bundles (specified in the "modules" or "out" setting)
        |automatically get normalization, so this setting does not apply to those
        |files.
      """.stripMargin
    )
    val requireBuildConfigOptimizeCss = settingKey[RequireOptimizeCss](
      """Allow CSS optimizations. Allowed values:
        |- "standard": @import inlining and removal of comments, unnecessary
        |whitespace and line returns.
        |Removing line returns may have problems in IE, depending on the type
        |of CSS.
        |- "standard.keepLines": like "standard" but keeps line returns.
        |- "none": skip CSS optimizations.
        |- "standard.keepComments": keeps the file comments, but removes line
        |returns.  (r.js 1.0.8+)
        |- "standard.keepComments.keepLines": keeps the file comments and line
        |returns. (r.js 1.0.8+)
        |- "standard.keepWhitespace": like "standard" but keeps unnecessary whitespace.
      """.stripMargin
    )
    val requireBuildConfigCssImportIgnore = settingKey[Seq[String]](
      """If "out" is not in the same directory as "cssIn", and there is a relative
        |url() in the cssIn file, use this to set a prefix URL to use. Only set it
        |if you find a problem with incorrect relative URLs after optimization.
      """.stripMargin
    )
    val requireBuildConfigCssPrefix = settingKey[Option[String]](
      """If optimizeCss is in use, a list of files to ignore for the @import
        |inlining. The value of this option should be a string of comma separated
        |CSS file names to ignore (like 'a.css,b.css'. The file names should match
        |whatever strings are used in the @import calls.
      """.stripMargin
    )
    val requireBuildConfigInlineText = settingKey[Boolean](
      """Inlines the text for any text! dependencies, to avoid the separate
        |async XMLHttpRequest calls to load those dependencies.
      """.stripMargin
    )
    val requireBuildConfigUseStrict = settingKey[Boolean](
      """Allow "use strict"; be included in the RequireJS files.
        |Default is false because there are not many browsers that can properly
        |process and give errors on code for ES5 strict mode,
        |and there is a lot of legacy code that will not work in strict mode.
      """.stripMargin
    )
    val requireBuildConfigNamespace = settingKey[Option[String]](
      """Allows namespacing requirejs, require and define calls to a new name.
        |This allows stronger assurances of getting a module space that will
        |not interfere with others using a define/require AMD-based module
        |system. The example below will rename define() calls to foo.define().
        |See http://requirejs.org/docs/faq-advanced.html#rename for a more
        |complete example.
      """.stripMargin
    )
    val requireBuildConfigSkipModuleInsertion = settingKey[Boolean](
      """If skipModuleInsertion is false, then files that do not use define()
        |to define modules will get a define() placeholder inserted for them.
        |Also, require.pause/resume calls will be inserted.
        |Set it to true to avoid this. This is useful if you are building code that
        |does not use require() in the built project or in the JS files, but you
        |still want to use the optimization tool from RequireJS to concatenate modules
        |together.
      """.stripMargin
    )
    val requireBuildConfigStubModules = settingKey[Seq[RequireId]](
      """Specify modules to stub out in the optimized file. The optimizer will
        |use the source version of these modules for dependency tracing and for
        |plugin use, but when writing the text into an optimized bundle, these
        |modules will get the following text instead:
        |If the module is used as a plugin:
        |    define({load: function(id){throw new Error("Dynamic load not allowed: " + id);}});
        |If just a plain module:
        |    define({});
        |This is useful particularly for plugins that inline all their resources
        |and use the default module resolution behavior (do *not* implement the
        |normalize() method). In those cases, an AMD loader just needs to know
        |that the module has a definition. These small stubs can be used instead of
        |including the full source for a plugin.
      """.stripMargin
    )
    val requireBuildConfigOptimizeAllPluginResources = settingKey[Boolean](
      """If it is not a one file optimization, scan through all .js files in the
        |output directory for any plugin resource dependencies, and if the plugin
        |supports optimizing them as separate files, optimize them. Can be a
        |slower optimization. Only use if there are some plugins that use things
        |like XMLHttpRequest that do not work across domains, but the built code
        |will be placed on another domain.
      """.stripMargin
    )
    val requireBuildConfigFindNestedDependencies = settingKey[Boolean](
      """Finds require() dependencies inside a require() or define call. By default
        |this value is false, because those resources should be considered dynamic/runtime
        |calls. However, for some optimization scenarios, it is desirable to
        |include them in the build.
        |Introduced in 1.0.3. Previous versions incorrectly found the nested calls
        |by default.
      """.stripMargin
    )
    val requireBuildConfigRemoveCombined = settingKey[Boolean](
      """If set to true, any files that were combined into a build bundle will be
        |removed from the output folder.
      """.stripMargin
    )
    val requireBuildConfigModules = settingKey[Seq[JS.Object]](
      """List the modules that will be optimized. All their immediate and deep
        |dependencies will be included in the module's file when the build is
        |done. If that module or any of its dependencies includes i18n bundles,
        |only the root bundles will be included unless the locale: section is set above.
      """.stripMargin
    )
    val requireBuildConfigPreserveLicenseComments = settingKey[Boolean](
      """By default, comments that have a license in them are preserved in the
        |output when a minifier is used in the "optimize" option.
        |However, for a larger built files there could be a lot of
        |comment files that may be better served by having a smaller comment
        |at the top of the file that points to the list of all the licenses.
        |This option will turn off the auto-preservation, but you will need
        |work out how best to surface the license information.
        |NOTE: As of 2.1.7, if using xpcshell to run the optimizer, it cannot
        |parse out comments since its native Reflect parser is used, and does
        |not have the same comments option support as esprima.
      """.stripMargin
    )
    val requireBuildConfigLogLevel = settingKey[RequireLogLevel](
      """Sets the logging level. """
    )
    val requireBuildConfigOnBuildRead = settingKey[Option[JavaScript]](
      """A function that if defined will be called for every file read in the
        |build that is done to trace JS dependencies. This allows transforms of
        |the content.
        |Ex:
        |onBuildRead: function (moduleName, path, contents) {
        |    //Always return a value.
        |    //This is just a contrived example.
        |    return contents.replace(/foo/g, 'bar');
        |}
      """.stripMargin
    )
    val requireBuildConfigOnBuildWrite = settingKey[Option[JavaScript]](
      """A function that will be called for every write to an optimized bundle
        |of modules. This allows transforms of the content before serialization.
        |Ex:
        |onBuildWrite: function (moduleName, path, contents) {
        |    //Always return a value.
        |    //This is just a contrived example.
        |    return contents.replace(/bar/g, 'foo');
        |}
      """.stripMargin
    )
    val requireBuildConfigOnModuleBundleComplete = settingKey[Option[JavaScript]](
      """A function that is called for each JS module bundle that has been
        |completed. This function is called after all module bundles have
        |completed, but it is called for each bundle. A module bundle is a
        |"modules" entry or if just a single file JS optimization, the
        |optimized JS file.
        |Introduced in r.js version 2.1.6
        |Ex:
        |onModuleBundleComplete: function (data) {
        |    /*
        |    data.name: the bundle name.
        |    data.path: the bundle path relative to the output directory.
        |    data.included: an array of items included in the build bundle.
        |    If a file path, it is relative to the output directory. Loader
        |    plugin IDs are also included in this array, but depending
        |    on the plugin, may or may not have something inlined in the
        |    module bundle.
        |    */
        |}
      """.stripMargin
    )
    val requireBuildConfigRawText = settingKey[RequireRawText](
      """Introduced in 2.1.3: Seed raw text contents for the listed module IDs.
        |These text contents will be used instead of doing a file IO call for
        |those modules. Useful if some module ID contents are dynamically
        |based on user input, which is common in web build tools.
        |Ex:
        |rawText: {
        |    'some/id': 'define(["another/id"], function () {});'
        |}
      """.stripMargin
    )
    val requireBuildConfigCjsTranslate = settingKey[Boolean](
      """Introduced in 2.0.2: if set to true, then the optimizer will add a
        |define(require, exports, module) {}); wrapper around any file that seems
        |to use commonjs/node module syntax (require, exports) without already
        |calling define(). This is useful to reuse modules that came from
        |or are loadable in an AMD loader that can load commonjs style modules
        |in development as well as AMD modules, but need to have a built form
        |that is only AMD. Note that this does *not* enable different module
        |ID-to-file path logic, all the modules still have to be found using the
        |requirejs-style configuration, it does not use node's node_modules nested
        |path lookups.
      """.stripMargin
    )
    val requireBuildConfigUseSourceUrl = settingKey[Boolean](
      """Introduced in 2.0.2: a bit experimental.
        |Each script in the build bundle will be turned into
        |a JavaScript string with a //# sourceURL comment, and then wrapped in an
        |eval call. This allows some browsers to see each evaled script as a
        |separate script in the script debugger even though they are all combined
        |in the same file. Some important limitations:
        |1) Do not use in IE if conditional comments are turned on, it will cause
        |errors:
        |http://en.wikipedia.org/wiki/Conditional_comment#Conditional_comments_in_JScript
        |2) It is only useful in optimize: 'none' scenarios. The goal is to allow
        |easier built bundle debugging, which goes against minification desires.
      """.stripMargin
    )
    val requireBuildConfigWaitSeconds = settingKey[Int](
      """Defines the loading time for modules. Depending on the complexity of the
        |dependencies and the size of the involved libraries, increasing the wait
        |interval may be required. Default is 7 seconds. Setting the value to 0
        |disables the waiting interval.
      """.stripMargin
    )
    val requireBuildConfigSkipSemiColonInsertion = settingKey[Boolean](
      """Introduced in 2.1.9: normally r.js inserts a semicolon at the end of a
        |file if there is not already one present, to avoid issues with
        |concatenated files and automatic semicolon insertion  (ASI) rules for
        |JavaScript. It is a very blunt fix that is safe to do, but if you want to
        |lint the build output, depending on the linter rules, it may not like it.
        |Setting this option to true skips this insertion. However, by doing this,
        |you take responsibility for making sure your concatenated code works with
        |JavaScript's ASI rules, and that you use a minifier that understands when
        |to insert semicolons to avoid ASI pitfalls.
      """.stripMargin
    )
    val requireBuildConfigKeepAmdefine = settingKey[Boolean](
      """Introduced in 2.1.10: if set to true, will not strip amdefine use:
        |https://github.com/requirejs/amdefine
        |Normally you should not need to set this. It is only a concern if using
        |a built .js file from some other source, that may have included amdefine
        |in the built input. If you get a build error like
        |"undefined is not a function" and the file that generated the error
        |references amdefine, then set this to true.
      """.stripMargin
    )
    val requireBuildConfigAllowSourceOverwrites = settingKey[Boolean](
      """Introduced in 2.1.11. As part of fixing a bug to prevent possible
        |overwrites of source code, https://github.com/jrburke/r.js/issues/444,
        |it prevented some cases where generated source is used for a build, and
        |it was OK to overwrite content in this source area as it was generated
        |from another source area, and not allowing source overwrites meant taking
        |another file copy hit. By setting this to true, it allows this sort of
        |source code overwriting. However, use at your own risk, and be sure you
        |have your configuration set correctly. For example, you may want to
        |set "keepBuildDir" to true.
      """.stripMargin
    )
    val requireBuildConfigWriteBuildTxt = settingKey[Boolean](
      """Introduced in 2.2.0. Default is true for compatibility with older
        |releases. If set to false, r.js will not write a build.txt file in the
        |"dir" directory when doing a full project optimization.
      """.stripMargin
    )
    val requireBuildConfig = settingKey[JS.Object]("The rjs build config")
    val requireBuildFile = settingKey[File]("The rjs build file")
    val requireBuildStage = taskKey[Pipeline.Stage]("Perform RequireJs optimization on the asset pipeline.")
  }

  import SbtRequire.autoImport._

  override val projectSettings =
    inConfig(Assets)(unscopedProjectSettings) ++
      inConfig(TestAssets)(unscopedProjectSettings) ++ Seq(
      requireVersion := "2.2.0",
      requirePath := {
        val path = if (requireCDN.value)
          s"//cdn.jsdelivr.net/webjars/requirejs/${requireVersion.value}/require"
        else
          s"/${webModulesLib.value}/requirejs/require"
        RequirePath.filename(RequirePath.minify(path, requireMinified.value))
      },
      requireOptimized := false,
      requireMinified := false,
      requireCDN := false,
      npmDevDependencies += "requirejs" -> requireVersion.value,
      libraryDependencies ++= Seq(
        "org.webjars" % "requirejs" % requireVersion.value
      )
    )

  private lazy val unscopedProjectSettings = Seq(

    //////////
    // MAIN //
    //////////

    requireMainIncludeFilter := AllPassFilter,
    requireMainExcludeFilter := NothingFilter,
    requireMainDirectory := sourceManaged.value / "requirejs",
    requireMainConfigBaseUrl := None,
    requireMainConfigPaths := Seq(
      requireMainModule.value -> RequirePath.minify(requireMainModule.value, requireMinified.value)
    ),
    requireMainConfigMinifiedPaths := Seq.empty,
    requireMainConfigCDNPaths := Seq.empty,
    requireMainConfigBundles := Nil,
    requireMainConfigShim := Nil,
    requireMainConfigMap := Nil,
    requireMainConfigConfig := Nil,
    requireMainConfigPackages := Nil,
    requireMainConfigNodeIdCompat := false,
    requireMainConfigWaitSeconds := 7,
    requireMainConfigContext := None,
    requireMainConfigDeps := Nil,
    requireMainConfigCallback := Some(
      JavaScript(s"""function() { require(['${requireMainIndexModule.value}']); }""")
    ),
    requireMainConfigEnforceDefine := false,
    requireMainConfigXhtml := false,
    requireMainConfigUrlArgs := None,
    requireMainConfigScriptType := "text/javascript",
    requireMainConfigSkipDataMain := false,
    requireMainConfig := JS.Object(
      "baseUrl" -> requireMainConfigBaseUrl.value,
      "paths" -> {
        val paths = requireMainConfigPaths.value ++
          (if (requireMinified.value) requireMainConfigMinifiedPaths.value else Seq.empty) ++
          (if (requireCDN.value) requireMainConfigCDNPaths.value else Seq.empty)
        JS.Object(
          paths.map {
            case (id, path) =>
              id -> JS(path)
          }: _*
        )
      },
      "bundles" -> JS.Object(requireMainConfigBundles.value.map {
        case (id, bundle) =>
          id -> JS(bundle)
      }: _*),
      "shim" -> JS.Object(requireMainConfigShim.value.map {
        case (id, RequireShimConfig(deps, exports, init)) =>
          id -> JS.Object(
            "deps" -> deps,
            "exports" -> exports,
            "init" -> init
          )
      }: _*),
      "map" -> requireMainConfigMap.value.groupBy(_._1).toSeq.map {
        case (k, v) =>
          k -> v.unzip._2.reduce(_ ++ _)
      },
      "config" -> JS.Object(requireMainConfigConfig.value: _*),
      "packages" -> requireMainConfigPackages.value.map {
        case (id, _package) =>
          id -> JS.Object(
            "name" -> _package.name,
            "main" -> _package.main
          )
      },
      "nodeIdCompat" -> requireMainConfigNodeIdCompat.value,
      "waitSeconds" -> requireMainConfigWaitSeconds.value,
      "context" -> requireMainConfigContext.value,
      "deps" -> requireMainConfigDeps.value,
      "callback" -> requireMainConfigCallback.value,
      "enforceDefine" -> requireMainConfigEnforceDefine.value,
      "xhtml" -> requireMainConfigXhtml.value,
      "urlArgs" -> requireMainConfigUrlArgs.value,
      "scriptType" -> requireMainConfigScriptType.value,
      "skipDataMain" -> requireMainConfigSkipDataMain.value
    ),
    requireMainModule := "main",
    requireMainPath := RequirePath.minify(
      requireMainConfigBaseUrl.value.getOrElse("") + "/" + requireMainModule.value,
      requireMinified.value
    ),
    requireMainFile := requireMainDirectory.value / RequirePath.filename(RequirePath.relativize(requireMainPath.value)),
    requireMainTemplateFile := None,
    requireMainGenerator := {
      implicit val logger = streams.value.log
      val configuration = requireMainConfig.value.js
      val moduleId = requireMainIndexModule.value
      val mainTemplate = requireMainTemplateFile.value.map(IO.read(_)) getOrElse {
        IO.readStream(getClass.getResource("/io/noplay/sbt/web/require/requirejs.js.ftl").openStream())
      }
      val mainFile = requireMainFile.value
      if (!mainFile.exists()) {
        mainFile.getParentFile.mkdirs()
        mainFile.createNewFile()
      }
      IO.write(
        mainFile,
        io.alphard.sbt.util.FreeMarker.render(
          mainTemplate,
          Map(
            "configuration" -> configuration,
            "moduleId" -> moduleId
          )
        )
      )
      Seq(mainFile)
    },
    includeFilter := includeFilter.value || requireMainIncludeFilter.value,
    excludeFilter := excludeFilter.value || requireMainExcludeFilter.value,
    managedSourceDirectories <+= requireMainDirectory,
    sourceGenerators <+= requireMainGenerator,
    webIndexScripts ++= Seq[Script](
      SbtWebIndex.autoImport.Script(
        requirePath.value,
        async = true,
        attributes = Map("data-main" -> RequirePath.filename(requireMainPath.value))
      )
    ),

    ///////////
    // BUILD //
    ///////////

    requireBuildIncludeFilter := AllPassFilter,
    requireBuildExcludeFilter := NothingFilter,
    requireBuildDirectory := target.value / "requirejs",
    requireBuildOptimizer := RequireOptimizer.Uglify2(),
    requireBuildConfigAppDir := requireBuildDirectory.value / "stage",
    requireBuildConfigBaseUrl <<= requireMainConfigBaseUrl,
    requireBuildConfigMainConfigFiles := Seq(
      requireBuildConfigAppDir.value / RequirePath.filename(requireMainPath.value)
    ),
    requireBuildConfigPaths := requireMainConfigPaths.value map {
      case (id, path) if path.indexOf("//") >= 0 => // do not optimize external path such as CDN urls
        (id, "empty:")
      case (id, path) if path.startsWith("/") => // rebase absolute path to app dir
        (id, (requireBuildConfigAppDir.value / RequirePath.relativize(path)).getAbsolutePath)
      case (id, path) =>
        (id, path)
    },
    requireBuildConfigMap <<= requireMainConfigMap,
    requireBuildConfigPackages <<= requireMainConfigPackages,
    requireBuildConfigDir := requireBuildDirectory.value / "build",
    requireBuildConfigKeepBuildDir := false,
    requireBuildConfigShim <<= requireMainConfigShim,
    requireBuildConfigWrapShim := false,
    requireBuildConfigLocale := None,
    requireBuildConfigSkipDirOptimize := false,
    requireBuildConfigGenerateSourceMaps := true,
    requireBuildConfigNormalizeDefines := RequireNormalizeDefines.Skip,
    requireBuildConfigOptimizeCss := RequireOptimizeCss.Standard(),
    requireBuildConfigCssImportIgnore := Seq.empty,
    requireBuildConfigCssPrefix := None,
    requireBuildConfigInlineText := true,
    requireBuildConfigUseStrict := false,
    requireBuildConfigNamespace := None,
    requireBuildConfigSkipModuleInsertion := false,
    requireBuildConfigStubModules := Seq.empty,
    requireBuildConfigOptimizeAllPluginResources := false,
    requireBuildConfigFindNestedDependencies := false,
    requireBuildConfigRemoveCombined := true,
    requireBuildConfigModules := Seq.empty,
    requireBuildConfigPreserveLicenseComments := false,
    requireBuildConfigLogLevel := RequireLogLevel.Trace,
    requireBuildConfigOnBuildRead := None,
    requireBuildConfigOnBuildWrite := None,
    requireBuildConfigOnModuleBundleComplete := None,
    requireBuildConfigRawText := Seq.empty,
    requireBuildConfigCjsTranslate := false,
    requireBuildConfigUseSourceUrl := false,
    requireBuildConfigWaitSeconds <<= requireMainConfigWaitSeconds,
    requireBuildConfigSkipSemiColonInsertion := false,
    requireBuildConfigKeepAmdefine := false,
    requireBuildConfigAllowSourceOverwrites := false,
    requireBuildConfigWriteBuildTxt := false,
    requireBuildConfig := JS.Object(
      "appDir" -> requireBuildConfigAppDir.value.getAbsolutePath,
      "baseUrl" -> requireBuildConfigBaseUrl.value.map {
        case baseUrl if baseUrl.startsWith("/") =>
          (requireBuildConfigAppDir.value / RequirePath.relativize(baseUrl)).getAbsolutePath
        case baseUrl =>
          baseUrl
      },
      "mainConfigFile" -> requireBuildConfigMainConfigFiles.value,
      "paths" -> JS.Object(requireBuildConfigPaths.value.map {
        case (id, path) =>
          id -> JS(path)
      }: _*
      ),
      "map" -> requireBuildConfigMap.value.groupBy(_._1).toSeq.map {
        case (k, v) =>
          k -> v.unzip._2.reduce(_ ++ _)
      },
      "packages" -> requireBuildConfigPackages.value.map {
        case (id, _package) =>
          id -> JS.Object(
            "name" -> _package.name,
            "main" -> _package.main
          )
      },
      "dir" -> requireBuildConfigDir.value,
      "keepBuildDir" -> requireBuildConfigKeepBuildDir.value,
      "shim" -> JS.Object(requireBuildConfigShim.value.map {
        case (id, RequireShimConfig(deps, exports, init)) =>
          id -> JS.Object(
            "deps" -> deps,
            "exports" -> exports,
            "init" -> init
          )
      }: _*),
      "wrapShim" -> requireBuildConfigWrapShim.value,
      "locale" -> requireBuildConfigLocale.value,
      "generateSourceMaps" -> requireBuildConfigGenerateSourceMaps.value,
      "normalizeDirDefines" -> (requireBuildConfigNormalizeDefines.value match {
        case RequireNormalizeDefines.All => "all"
        case RequireNormalizeDefines.Skip => "skip"
      }),
      "optimizeCss" -> (requireBuildConfigOptimizeCss.value match {
        case RequireOptimizeCss.None => "none"
        case RequireOptimizeCss.Standard(keepLines, keepComments, keepWhitespace) =>
          "standard" +
            (if (keepLines) ".keepLines" else "") +
            (if (keepComments) ".keepComments" else "") +
            (if (keepWhitespace) ".keepWhitespace" else "")
      }),
      "cssImportIgnore" -> requireBuildConfigCssImportIgnore.value.mkString(","),
      "cssPrefix" -> requireBuildConfigCssPrefix.value,
      "useStrict" -> requireBuildConfigUseStrict.value,
      "namespace" -> requireBuildConfigNamespace.value,
      "skipModuleInsertion" -> requireBuildConfigSkipModuleInsertion.value,
      "stubModules" -> requireBuildConfigStubModules.value,
      "optimizeAllPluginResources" -> requireBuildConfigOptimizeAllPluginResources.value,
      "findNestedDependencies" -> requireBuildConfigFindNestedDependencies.value,
      "removeCombined" -> requireBuildConfigRemoveCombined.value,
      "modules" -> requireBuildConfigModules.value,
      "preserveLicenseComments" -> requireBuildConfigPreserveLicenseComments.value,
      "logLevel" -> (requireBuildConfigLogLevel.value match {
        case RequireLogLevel.Trace => 0
        case RequireLogLevel.Info => 1
        case RequireLogLevel.Warn => 2
        case RequireLogLevel.Error => 3
        case RequireLogLevel.Silent => 4
      }),
      "onBuildRead" -> requireBuildConfigOnBuildRead.value,
      "onBuildWrite" -> requireBuildConfigOnBuildWrite.value,
      "onModuleBundleComplete" -> requireBuildConfigOnModuleBundleComplete.value,
      "rawText" -> requireBuildConfigRawText.value.map {
        case (id, javascript) =>
          id -> javascript
      },
      "cjsTranslate" -> requireBuildConfigCjsTranslate.value,
      "useSourceUrl" -> requireBuildConfigUseSourceUrl.value,
      "waitSeconds" -> requireBuildConfigWaitSeconds.value,
      "skipSemiColonInsertion" -> requireBuildConfigSkipSemiColonInsertion.value,
      "keepAmdefine" -> requireBuildConfigKeepAmdefine.value,
      "allowSourceOverwrites" -> requireBuildConfigAllowSourceOverwrites.value,
      "writeBuildTxt" -> requireBuildConfigWriteBuildTxt.value
    ) ++ {
      requireBuildOptimizer.value match {
        case RequireOptimizer.Uglify(config) =>
          JS.Object(
            "optimize" -> "uglify",
            "uglify" -> config
          )
        case RequireOptimizer.Uglify2(config) =>
          JS.Object(
            "optimize" -> "uglify2",
            "uglify2" -> config
          )
        case RequireOptimizer.Closure(config) =>
          JS.Object(
            "optimize" -> "closure",
            "closure" -> config
          )
      }
    },
    requireBuildFile := requireBuildDirectory.value / "build.js",
    requireBuildStage := Def.task[Pipeline.Stage] {
      mappings =>
        val include = requireBuildIncludeFilter.value
        val exclude = requireBuildExcludeFilter.value

        val optimizerMappings = mappings.filter(f => !f._1.isDirectory && include.accept(f._1) && !exclude.accept(f._1))

        SbtWeb.syncMappings(
          streams.value.cacheDirectory,
          "rjs-sync",
          optimizerMappings,
          requireBuildConfigAppDir.value
        )

        IO.write(requireBuildFile.value, requireBuildConfig.value.js, Charset.forName("UTF-8"))

        val cacheDirectory = streams.value.cacheDirectory / "rjs-cache"
        val runUpdate = FileFunction.cached(cacheDirectory, FilesInfo.hash) {
          _ =>
            streams.value.log.info("Optimizing JavaScript with RequireJS")

            SbtJsTask.executeJs(
              state.value,
              (JsEngineKeys.engineType in requireBuildStage).value,
              (JsEngineKeys.command in requireBuildStage).value,
              Nil,
              npmModulesDirectory.value / "requirejs" / "bin" / "r.js",
              Seq("-o", requireBuildFile.value.getAbsolutePath),
              (JsTaskKeys.timeoutPerSource in requireBuildStage).value * optimizerMappings.size
            )

            requireBuildConfigDir.value.***.get.toSet
        }

        val optimizedMappings = runUpdate(requireBuildConfigAppDir.value.***.get.toSet).filter(_.isFile).pair(relativeTo(requireBuildConfigDir.value))
        (mappings.toSet -- optimizerMappings.toSet ++ optimizedMappings).toSeq
    }.dependsOn(webJarsNodeModules in Plugin).value,
    pipelineStages ++= (if ((requireOptimized in Assets).value) Seq(requireBuildStage) else Nil)
  )
}