// Declarative pipeline: Git Checkout -> Test -> Build -> Archive Artifact
// Works on Linux agents (sh) and Windows agents (bat).
def run(String cmd) {
    if (isUnix()) { sh cmd } else { bat cmd }
}

pipeline {
    agent any

    triggers {
        githubPush()                 // GitHub webhook trigger
        pollSCM('H/2 * * * *')       // fallback: poll the repo every ~2 minutes
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Git Checkout') {
            steps {
                checkout scm
            }
        }

        // CI enhancement: automated unit tests. A failing test fails the build.
        stage('Test') {
            steps {
                run('mvn -B clean test')
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build') {
            steps {
                run('mvn -B package -DskipTests')
            }
        }

        stage('Archive Artifact') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
    }

    post {
        success { echo 'Build, tests and archive completed successfully.' }
        failure { echo 'Pipeline failed - check the Test or Build stage output.' }
    }
}
