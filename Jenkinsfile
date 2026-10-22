// Declarative pipeline for the Policyholder Grievance Portal.
// Runs on the Windows Jenkins service (http://localhost:8081).
// DEPLOY_TARGET=docker (default): builds a versioned image, pushes it to the registry and replaces the running container.
// DEPLOY_TARGET=tomcat: copies the WAR into the local Tomcat (http://localhost:8080).
pipeline {
    agent any

    parameters {
        choice(
            name: 'DEPLOY_TARGET',
            choices: ['docker', 'tomcat'],
            description: 'docker: build, push and run a container on port 8082. tomcat: copy the WAR into the local Tomcat on port 8080.'
        )
        string(
            name: 'REGISTRY',
            defaultValue: 'localhost:5000',
            description: 'Registry that receives the image (DEPLOY_TARGET=docker). The local registry container listens on localhost:5000.'
        )
        choice(
            name: 'DEPLOY_ENV',
            choices: ['local', 'staging'],
            description: 'Target environment. "local" deploys /grievance-portal, "staging" deploys /grievance-portal-staging. Only one of them is kept on this Tomcat at a time because both would use the same H2 database file.'
        )
    }

    triggers {
        // A new commit on the branch starts a run by itself (a webhook needs a public address).
        pollSCM('H/2 * * * *')
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        // Machine-specific locations: change these lines if the tools live elsewhere.
        // The Jenkins service does not inherit the PATH of the logged-in user, so the folders are added here.
        MAVEN_BIN   = 'C:\\DevTools\\Maven\\apache-maven-3.9.16\\bin'
        DOCKER_BIN  = 'C:\\Users\\nandi\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin'
        TOMCAT_HOME = 'C:\\tools\\apache-tomcat-10.1.61'
        PATH        = "${MAVEN_BIN};${DOCKER_BIN};${env.PATH}"
        TOMCAT_URL  = 'http://localhost:8080'
        IMAGE_NAME     = 'grievance-portal'
        CONTAINER_NAME = 'grievance-portal'
        CONTAINER_PORT = '8082'
        APP_CONTEXT = "${params.DEPLOY_ENV == 'staging' ? 'grievance-portal-staging' : 'grievance-portal'}"
        OTHER_CONTEXT = "${params.DEPLOY_ENV == 'staging' ? 'grievance-portal' : 'grievance-portal-staging'}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                bat 'git log -1 --oneline'
                script {
                    // The image tag names the build and the commit, for example b12-3f9c1a2.
                    def sha = bat(returnStdout: true, script: '@git rev-parse --short HEAD').trim()
                    env.IMAGE_TAG = "b${env.BUILD_NUMBER}-${sha}"
                    env.IMAGE_REF = "${params.REGISTRY}/${env.IMAGE_NAME}:${env.IMAGE_TAG}"
                    echo "Image for this build: ${env.IMAGE_REF}"
                }
            }
        }

        stage('Docker check') {
            when { expression { params.DEPLOY_TARGET == 'docker' } }
            steps {
                // Fails early, before the long build, if Jenkins cannot reach the Docker engine.
                bat 'docker version'
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

        stage('Test') {
            steps {
                // Starts a throw-away Tomcat on port 8090 with an in-memory database, runs the Selenium tests
                // against it and stops it again. The real Tomcat on 8080 is not touched.
                bat 'mvn -B verify -Pci-ui -Dskip.unit=true -Dui.noSandbox=true'
            }
            post {
                always {
                    junit testResults: 'target/failsafe-reports/TEST-*.xml', allowEmptyResults: true
                    archiveArtifacts artifacts: 'target/screenshots/**', allowEmptyArchive: true
                }
            }
        }

        stage('Image') {
            when { expression { params.DEPLOY_TARGET == 'docker' } }
            steps {
                bat 'docker build --label git.commit=%IMAGE_TAG% --label jenkins.build=%BUILD_NUMBER% -t %IMAGE_REF% .'
                bat 'docker images %REGISTRY%/%IMAGE_NAME%'
            }
        }

        stage('Push') {
            when { expression { params.DEPLOY_TARGET == 'docker' } }
            steps {
                bat 'docker push %IMAGE_REF%'
                // Lists the tags the registry now holds, for the build log.
                bat 'curl -s http://%REGISTRY%/v2/%IMAGE_NAME%/tags/list & exit /b 0'
            }
        }

        stage('Run container') {
            when { expression { params.DEPLOY_TARGET == 'docker' } }
            steps {
                // Only reached when Build, Package and Test passed. The old container is replaced by one that
                // is started from the image that was just pulled back from the registry.
                bat '''
                    docker pull %IMAGE_REF% || exit /b 1
                    docker rm -f %CONTAINER_NAME% 2>nul
                    docker run -d --name %CONTAINER_NAME% -p %CONTAINER_PORT%:8080 -v grievance-data:/usr/local/tomcat/data -e GP_ENV_NAME=docker-build-%BUILD_NUMBER% %IMAGE_REF% || exit /b 1
                    docker ps --filter name=%CONTAINER_NAME%
                '''
            }
        }

        stage('Deploy') {
            when { expression { params.DEPLOY_TARGET == 'tomcat' } }
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
                    if ($env:DEPLOY_TARGET -eq 'docker') {
                        $url = "http://localhost:$env:CONTAINER_PORT/grievance-portal/health"
                    } else {
                        $url = "$env:TOMCAT_URL/$env:APP_CONTEXT/health"
                    }
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
        success {
            script {
                if (params.DEPLOY_TARGET == 'docker') {
                    echo "Container ${env.CONTAINER_NAME} now runs ${env.IMAGE_REF}. Open http://localhost:${env.CONTAINER_PORT}/grievance-portal/"
                } else {
                    echo "Deployed. Open ${env.TOMCAT_URL}/${env.APP_CONTEXT}/"
                }
            }
        }
        failure { echo 'Pipeline failed - see the stage that turned red.' }
    }
}
