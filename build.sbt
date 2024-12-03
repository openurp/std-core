import org.openurp.parent.Dependencies.*
import org.openurp.parent.Settings.*

ThisBuild / version := "0.0.14"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/openurp/std-core"),
    "scm:git@github.com:openurp/std-core.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id = "chaostone",
    name = "Tihua Duan",
    email = "duantihua@gmail.com",
    url = url("http://github.com/duantihua")
  )
)

ThisBuild / description := "OpenURP Std Core Library"
ThisBuild / homepage := Some(url("http://openurp.github.io/std-core/index.html"))

val apiVer = "0.41.13"
val starterVer = "0.3.47"
val eduCoreVer = "0.3.6"

val openurp_edu_api = "org.openurp.edu" % "openurp-edu-api" % apiVer
val openurp_std_api = "org.openurp.std" % "openurp-std-api" % apiVer
val openurp_edu_core = "org.openurp.edu" % "openurp-edu-core" % eduCoreVer
val openurp_stater_ws = "org.openurp.starter" % "openurp-starter-ws" % starterVer

lazy val root = (project in file("."))
  .settings(common)
  .aggregate(core)

lazy val core = (project in file("core"))
  .settings(
    name := "openurp-std-core",
    organization := "org.openurp.std",
    common,
    libraryDependencies ++= Seq(openurp_std_api, openurp_edu_api),
    libraryDependencies ++= Seq(beangle_ems_app,openurp_edu_core)
  )

publish / skip := true
