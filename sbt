#!/bin/bash
#------------------------------------------------------------------
# sbt driver script. See "./sbt --help" for more information.
# Notes:
# Thanks to "tcn" for suggesting the --debug option.
#------------------------------------------------------------------

jettyport=
jmxport=

function showhelp() {
  cat <<EOF
AkkaWebSampleExercise/sbt - Drive SBT for the AkkaWebSampleExercise 
(See http://github.com/deanwampler/AkkaWebSampleExercise)
usage:
  ./sbt [-h|--help] [--jettyport port] [--jmxport port] [--debug port] [--maxheap NNNM] [--maxrecs N] \
      [--inmemory | --mongodb] [-Dprop=value ...] [sbt_args]
where:
  -h | --help       Show this help message.
  --jettyport port  Run Jetty on the specified port (default: Jetty's default setting - 8080).
  --jmxport port    Enable JMX access on the specified port.
  --debug port      Run JVM in debug mode on specified port
  --maxheap NNNM    Override the default values for the heap size. Append the units, e.g., "M". (default: 1024M).
  --inmemory        Use in-memory data storage only. Use if you don't want to bother with Mongodb (default set in akka.conf).
  --mongodb         Use MongoDB-backed data storage (default set in akka.conf).
  -Dprop=value      Define a Java property that is passed on to the JVM.
  sbt_args          Any other arguments are passed to SBT itself. They must come after the other script arguments.
EOF
}

maxheap=1024M

while [ $# -ne 0 ]
do
  case $1 in
    help|-h*|--h*)
      showhelp
      exit 0
      ;;
    --jettyport)
      shift
      jettyport=$1
      ;;
    --maxheap)
      shift
      maxheap=$1
      ;;
    --jmxport)
      shift
      jmxport=$1
      ;;
    --debug)
      shift
      debug=$1
      ;;
    --inmemory)
      storage=$1
      ;;
    --mongodb)
      storage=$1
      ;;
    -D*)
      JAVA_OPTIONS="$1 $JAVA_OPTIONS"
      ;;
    *)
      break
      ;;
  esac
  shift
done

# Unset AKKA_HOME, if defined, so the user's installation is ignored. All Akka dependencies are 
# managed through SBT's maven compatibility.
export AKKA_HOME=""

if [ -n "$jmxport" ] ; then
  echo "JMX monitoring enabled on port $jmxport, without authentication."
  JAVA_OPTIONS="-Dcom.sun.management.jmxremote.port=$jmxport -Dcom.sun.management.jmxremote.authenticate=false $JAVA_OPTIONS"
fi
if [ -n "$jettyport" ] ; then
  echo "Jetty port: $jettyport."
  JAVA_OPTIONS="-Djetty.port=$jettyport $JAVA_OPTIONS"
fi
if [ -n "$storage" ] ; then
  echo "Using data storage option: $storage."
  JAVA_OPTIONS="-Dapp.datastore.type=$storage $JAVA_OPTIONS"
fi
if [ -n "$debug" ] ; then
 echo "Running in debug mode, port: $debug"
 JAVA_OPTIONS="$JAVA_OPTIONS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=$debug"
fi

# -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=256m is supposed to reduce PermGen errors.
echo env java $JAVA_OPTIONS -Xmx$maxheap -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=256m -jar ./misc/sbt-launch-0.7.4.jar "$@"
env java $JAVA_OPTIONS -Xmx$maxheap -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=256m -jar ./misc/sbt-launch-0.7.4.jar "$@"
