pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr: "50"))
        timestamps ()
    }
    environment {
        DEPLOY_PROD = "YES"
        EC2_HOST_DEV = "52.69.115.46"
        DB_HOST_DEV = "localhost"
        EC2_HOST_UAT = "54.65.64.11"
        DB_HOST_UAT = "localhost"
        EC2_HOST_PRODUCTION_WEB1 = "172.31.13.52"
        EC2_HOST_PRODUCTION_WEB2 = "172.31.12.55"
        DB_HOST_PRODUCTION = "172.31.4.20"
    }
    stages {
        stage("Run jslint") {
            steps {
                sh '''
                    cd modules/openlmis-web/
                    rm -rf node_modules/
                    npm install
                    npm run jslint
                    npm run css_lint
                    npm run less uglify
                    cd ../../
                '''
            }
        }
        stage("Build war") {
            tools {
               jdk "jdk-1.7"
            }
            steps {
                sh '''
                    java -version
                    ./gradlew clean :modules:openlmis-web:removePropertiesFiles
                    ./gradlew war -x :modules:openlmis-web:less -x :modules:openlmis-web:jsHint
                '''
            }
        }
        stage("Deploy To Dev") {
            when {
                branch "master"
            }
            steps {
                runFlyway("${EC2_HOST_DEV}", "${DB_HOST_DEV}")
                deploy("${EC2_HOST_DEV}")
            }
        }
        stage("Deploy To UAT") {
            when {
                branch "release"
            }
            steps {
                runFlyway("${EC2_HOST_UAT}", "${DB_HOST_UAT}")
                deploy("${EC2_HOST_UAT}")
            }
        }
        stage("Approval of deploy to Production") {
            when {
                branch "release"
            }
            steps {
                script {
                    try {
                        timeout (time: 15, unit: "MINUTES") {
                            input message: "Do you want to proceed for Production deployment?"
                        }
                    }
                    catch (error) {
                        if ("${error}".startsWith("org.jenkinsci.plugins.workflow.steps.FlowInterruptedException")) {
                            currentBuild.result = "SUCCESS" // Build was aborted
                        }
                        env.DEPLOY_PROD = "NO"
                    }
                }
            }
        }
        stage("Deploy To Production") {
            when {
                allOf {
                    branch "release"
                    environment name: "DEPLOY_PROD", value: "YES"
                }
            }
            steps {
                runFlyway("${EC2_HOST_PRODUCTION_WEB1}", "${DB_HOST_PRODUCTION}")
                deploy("${EC2_HOST_PRODUCTION_WEB1}")
                deploy("${EC2_HOST_PRODUCTION_WEB2}")
            }
        }
    }
}

def runFlyway(ec2_host, db_host) {
    withEnv(["EC2_HOST=${ec2_host}", "DB_HOST=${db_host}"]) {
        withCredentials([usernamePassword(credentialsId: "db-info", usernameVariable: "USER", passwordVariable: "PASS")]) {
            sshagent(["uat-ssh-key"]) {
                sh '''
                    echo "run flyway"
                    scp modules/db/build/libs/db.jar ubuntu@${EC2_HOST}:~/artifacts/
                    scp modules/migration/build/libs/migration.jar ubuntu@${EC2_HOST}:~/artifacts/
                    ssh -tt -o StrictHostKeyChecking=no ubuntu@${EC2_HOST} << EOF
echo "backup db.jar, migration.jar"
sudo mv /app/tomcat/openlmis/db/db.jar /app/tomcat/openlmis/artifact_last/
sudo mv /app/tomcat/openlmis/db/migration.jar /app/tomcat/openlmis/artifact_last/

echo "copy db.jar, migration.jar"
sudo cp ~/artifacts/db.jar /app/tomcat/openlmis/db/
sudo cp ~/artifacts/migration.jar /app/tomcat/openlmis/db/

echo "flyway migration"
sudo rm -rf /opt/flyway-commandline/sql/db/* /opt/flyway-commandline/sql/migration/*
sudo unzip -q /app/tomcat/openlmis/db/db.jar -d /opt/flyway-commandline/sql/db
sudo unzip -q /app/tomcat/openlmis/db/migration.jar -d /opt/flyway-commandline/sql/migration
sudo /opt/flyway-commandline/flyway -url=jdbc:postgresql://${DB_HOST}:5432/open_lmis -schemas=public,atomfeed -user=${USER} -password=${PASS} -table=schema_version -placeholderReplacement=false -locations=filesystem:/opt/flyway-commandline/sql/db -X migrate
sudo /opt/flyway-commandline/flyway -url=jdbc:postgresql://${DB_HOST}:5432/open_lmis -schemas=public,atomfeed -user=${USER} -password=${PASS} -table=migration_schema_version -placeholderReplacement=false -baselineOnMigrate=true -locations=filesystem:/opt/flyway-commandline/sql/migration -X migrate
exit
                EOF'''
            }
        }
    }
}

def deploy(ec2_host) {
    withEnv(["EC2_HOST=${ec2_host}"]) {
        sshagent(["uat-ssh-key"]) {
            sh '''
                echo "deploy service"
                scp modules/openlmis-web/build/libs/openlmis-web.war ubuntu@${EC2_HOST}:~/artifacts/
                ssh -tt -o StrictHostKeyChecking=no ubuntu@${EC2_HOST} << EOF
echo "stop tomcat"
sudo service tomcat stop
sleep 5

echo "backup last ROOT.war"
sudo cp /app/tomcat/server/webapps/ROOT.war /app/tomcat/openlmis/artifact_last/

echo "replace ROOT.war"
sudo rm -rf /app/tomcat/server/webapps/ROOT.war /app/tomcat/server/webapps/ROOT
sudo cp ~/artifacts/openlmis-web.war /app/tomcat/server/webapps/ROOT.war

echo "start tomcat"
sudo service tomcat start
sleep 5
ps -ef | grep java
exit
            EOF'''
        }
    }
}
