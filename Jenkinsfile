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
                sh 'mvn test'
            }
        }

        // stage('Static Analysis') {
        //     steps {
        //         script {
        //         }
        //     }
        // }
    }
    post {
        always {
            // def scannerHome = tool 'SonarQube Scanner 3.3';
            withSonarQubeEnv {
                sh "mvn ${env.SONAR_MAVEN_GOAL} -Dsonar.host.url=${env.SONAR_HOST_URL}"
            }
            junit 'target/surefire-reports/*.xml'
        }
    }
    stages {
        stage('Package') {
            steps {
                sh 'mvn package'
                archiveArtifacts 'target/*.hpi'
            }
        }
    }
}
