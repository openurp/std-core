import org.openurp.parent.Dependencies.*
import org.openurp.parent.Settings.*

ThisBuild / version := "0.0.8"

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

val apiVer = "0.41.0"
val starterVer = "0.3.38"
val eduCoreVer = "0.2.12"

val openurp_edu_api = "org.openurp.edu" % "openurp-edu-api" % apiVer
val openurp_std_api = "org.openurp.std" % "openurp-std-api" % apiVer
val openurp_edu_core = "org.openurp.edu" % "openurp-edu-core" % eduCoreVer
val openurp_stater_ws = "org.openurp.starter" % "openurp-starter-ws" % starterVer

lazy val root = (project in file("."))
  .settings()
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
