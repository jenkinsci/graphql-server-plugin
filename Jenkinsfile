#!groovy

if (JENKINS_URL == 'https://ci.jenkins.io/') {
    buildPlugin(configurations: buildPlugin.recommendedConfigurations())
    return
}

pipeline {
    agent {
        docker {
            image 'maven:3.6-jdk-8'
        }
    }
    options {
        timeout(time: 10, unit: 'MINUTES')
        ansiColor('xterm')
    }

    stages {
        stage('Build') {
            steps {
                sh 'mvn compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn verify test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'

                    script {
                        // def scannerHome = tool 'SonarQube Scanner 3.3';
                        withSonarQubeEnv {
                            sh "mvn ${env.SONAR_MAVEN_GOAL} -Dsonar.host.url=${env.SONAR_HOST_URL}"
                        }
                    }
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package'
                archiveArtifacts 'target/*.hpi'
            }
        }
    }
}
