#!groovy

if (JENKINS_URL == 'https://ci.jenkins.io/') {
    buildPlugin(
      configurations: buildPlugin.recommendedConfigurations()
    )
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

    stage('Static Analysis') {
        steps {
            script {
                // def scannerHome = tool 'SonarQube Scanner 3.3';
                withSonarQubeEnv {
                    sh "mvn $SONAR_MAVEN_GOAL -Dsonar.host.url=$SONAR_HOST_URL"
                }
            }
        }
    }

    stage('Test') {
      steps {
        sh 'mvn test'
      }
    }

    stage('Package') {
      steps {
        sh 'mvn package'
      }
    }
  }
}
