import com.cisco.maglev.*

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    node(config.nodeName) {
        env.LC_ALL = "C.UTF-8"
        env.LANG = "C.UTF-8"
        Maglev maglev = new Maglev()
        
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            
        def utils = new Utils()
        BuildConfig buildConfig = utils.getBuildConfig()

        env.workspace = pwd()
        buildConfig.workspace = env.workspace
        env.no_proxy = "localhost,cisco.com"
        env.NO_PROXY = "localhost,cisco.com"
        def factory = new com.cisco.maglev.BuilderFactory()
        def builder = factory.get_builder(config.serviceType)

       
       
        buildConfig.appstack = env.APPSTACK
       
        // cluster creation config
        buildConfig.cluster = env.CLUSTER_IP
        // buildConfig.username= env.Username
       //def username = "maglev"
        // release branch details
        buildConfig.releaseJobType = config.frequency
        buildConfig.commonVersion = "1.0.0.0.1"

        // setting gerrit ref for this job to NA , we can think to support patchset based trigger in future
        buildConfig.grrritRef = "NA"


        env.https_proxy = "http://proxy.esl.cisco.com:80/"
        env.no_proxy = "cisco.com,localhost,10.195.127.40,10.195.127.62,${buildConfig.cluster}"

    
       

            stage("Deploy ${buildConfig.appstack}") {
                echo "# Deploying appstack: $buildConfig.appstack"
                withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: 'test_maglev',
                      usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                    
                   ARTIFACT_VERSION = sh returnStdout: true, script: '''curl http://maglev-fileserver.cisco.com/artifacts/daily-release/platform-ui/daily/ | grep -Eo \'<a [^>]+>\' | sed "s/<a href=\\"//g" | sed "s/\\">//g"'''
                   echo ARTIFACT_VERSION

                   def selected_artifact_version = input message: 'choose the artifact version', parameters: [choice(choices: ['${ARTIFACT_VERSION}'], description: '', name: 'artifact_version')]
                    echo selected_artifact_version

                   ARTIFACT_TARFILE=sh returnStdout: true, script: """curl http://maglev-fileserver.cisco.com/artifacts/daily-release/platform-ui/daily/${selected_artifact_version}/ | grep -Eo '<a [^>]+>' | sed "s/<a href=\"//g" | sed "s/\">//g" | grep "tar" """
                   echo ARTIFACT_TARFILE
                   
                   def selected_tarfile = input message: 'choose the artifact tarfile', parameters: [choice(choices: ['${ARTIFACT_TARFILE}'], description: '', name: 'artifact_tarfile')]
                   echo selected_tarfile
                   sh"""
                    Script = " pwd; mkdir tmp; cd tmp; wget http://maglev-fileserver.cisco.com/artifacts/daily-release/platform-ui/daily/${selected_artifact_version}/${selected_tarfile}; tar -xvzf ${selected_tarfile}; sudo cp -r opt/* /opt/; cd /opt/maglev/catalog/platform-ui; pwd; ls; maglev catalog push ./;"
                    sshpass -p ${PASSWORD} ssh -l ${USERNAME} ${buildConfig.cluster} "${Script}"
                   """
            
                    /*if (buildConfig.testType != "testonly") {
                        utils.push_and_deploy_packages(buildConfig, "push")
                        utils.push_and_deploy_packages(buildConfig, "deploy")
                    }*/
            }   
                    }
    



     
            // buildConfig.appstack = packages
            // echo "# Bundling Package: ${buildConfig.appstack}"
            // stage("Bundle ${buildConfig.appstack}") {
            //     if (buildConfig.releaseJobType == "daily" && buildConfig.publish == "true" && buildConfig.testType != "testonly") {
            //         try {
            //             String args = "${env.BUILD_ID} ${buildConfig.releasebranch} ${buildConfig.releaseJobType} ${buildConfig.commonVersion} ${buildConfig.appstack}"
            //             utils.runScript(buildConfig.workspace, "scripts/bundle_artifacts_common.sh", args)
            //         } catch (err) {
            //             echo "# ${buildConfig.appstack} Bundle failed"
            //             notifier.pre_build_notify(buildConfig, "Failed")
            //             throw err
            //         }
            //     }
            // }

          
      

          
    }
}
