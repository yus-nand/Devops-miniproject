// Declarative pipeline for the Policyholder Grievance Portal.
// Runs on the Windows Jenkins service (http://localhost:8081) and deploys to the local Tomcat (http://localhost:8080).
pipeline {
    agent any

    parameters {
        choice(
            name: 'DEPLOY_ENV',
            choices: ['local', 'staging'],
            description: 'Target environment. "local" deploys /grievance-portal, "staging" deploys /grievance-portal-staging. Only one of them is kept on this Tomcat at a time because both would use the same H2 database file.'
        )
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        // Machine-specific locations: change these two lines if the tools live elsewhere.
        MAVEN_BIN   = 'C:\\DevTools\\Maven\\apache-maven-3.9.16\\bin'
        TOMCAT_HOME = 'C:\\tools\\apache-tomcat-10.1.61'
        PATH        = "${MAVEN_BIN};${env.PATH}"
        TOMCAT_URL  = 'http://localhost:8080'
        APP_CONTEXT = "${params.DEPLOY_ENV == 'staging' ? 'grievance-portal-staging' : 'grievance-portal'}"
        OTHER_CONTEXT = "${params.DEPLOY_ENV == 'staging' ? 'grievance-portal' : 'grievance-portal-staging'}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                bat 'git log -1 --oneline'
            }
        }

        stage('Build') {
            steps {
                bat 'mvn -B clean test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Package') {
            steps {
                bat 'mvn -B package -DskipTests'
                archiveArtifacts artifacts: 'target/grievance-portal.war', fingerprint: true
            }
        }

        stage('Deploy') {
            steps {
                echo "Deploying to environment '${params.DEPLOY_ENV}' as /${env.APP_CONTEXT}"
                powershell '''
                    $ErrorActionPreference = 'Stop'
                    $webapps = Join-Path $env:TOMCAT_HOME 'webapps'
                    if (-not (Test-Path $webapps)) { throw "Tomcat webapps folder not found: $webapps" }

                    # Remove the other environment and the previous copy of this one, so a stale build cannot answer the health check.
                    foreach ($name in @($env:OTHER_CONTEXT, $env:APP_CONTEXT)) {
                        Remove-Item -Force -ErrorAction SilentlyContinue (Join-Path $webapps "$name.war")
                        Remove-Item -Recurse -Force -ErrorAction SilentlyContinue (Join-Path $webapps $name)
                    }

                    # Wait (up to 60 s) for Tomcat to notice the removal.
                    for ($i = 0; $i -lt 20; $i++) {
                        try { Invoke-WebRequest -Uri "$env:TOMCAT_URL/$env:APP_CONTEXT/health" -UseBasicParsing -TimeoutSec 3 | Out-Null }
                        catch { break }
                        Start-Sleep -Seconds 3
                    }

                    Copy-Item -Force 'target\\grievance-portal.war' (Join-Path $webapps "$env:APP_CONTEXT.war")
                    Write-Host "Copied WAR to $webapps\\$env:APP_CONTEXT.war"
                '''
            }
        }

        stage('Verify') {
            steps {
                powershell '''
                    $url = "$env:TOMCAT_URL/$env:APP_CONTEXT/health"
                    Write-Host "Waiting for $url"
                    for ($i = 1; $i -le 30; $i++) {
                        try {
                            $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
                            if ($r.Content -match '"status":"UP"') { Write-Host "Application is up: $($r.Content)"; exit 0 }
                        } catch { }
                        Start-Sleep -Seconds 3
                    }
                    Write-Error "Application did not report UP within 90 seconds"
                    exit 1
                '''
            }
        }
    }

    post {
        success { echo "Deployed. Open ${env.TOMCAT_URL}/${env.APP_CONTEXT}/" }
        failure { echo 'Pipeline failed - see the stage that turned red.' }
    }
}
