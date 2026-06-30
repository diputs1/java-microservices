def services = [
    [name: 'gateway-service',        module: 'src/gateway-service',            dockerfile: 'src/gateway-service/Dockerfile',             compose: 'gateway-service',        port: '5000'],
    [name: 'identity-service',       module: 'src/Services/identity-service',  dockerfile: 'src/Services/identity-service/Dockerfile',   compose: 'identity-service',       port: '5001'],
    [name: 'product-service',        module: 'src/Services/product-service',   dockerfile: 'src/Services/product-service/Dockerfile',    compose: 'product-service',        port: '5002'],
    [name: 'customer-service',       module: 'src/Services/customer-service',  dockerfile: 'src/Services/customer-service/Dockerfile',   compose: 'customer-service',       port: '5003'],
    [name: 'basket-service',         module: 'src/Services/basket-service',    dockerfile: 'src/Services/basket-service/Dockerfile',     compose: 'basket-service',         port: '5004'],
    [name: 'ordering-service',       module: 'src/Services/ordering-service',  dockerfile: 'src/Services/ordering-service/Dockerfile',   compose: 'ordering-service',       port: '5005'],
    [name: 'inventory-service',      module: 'src/Services/inventory-service', dockerfile: 'src/Services/inventory-service/Dockerfile',  compose: 'inventory-service',      port: '5006'],
    [name: 'background-job-service', module: 'src/background-job-service',     dockerfile: 'src/background-job-service/Dockerfile',      compose: 'background-job-service', port: '5007'],
    [name: 'email-service',          module: 'src/Services/email-service',     dockerfile: 'src/Services/email-service/Dockerfile',      compose: 'email-service',          port: '5008']
]

def serviceByName(String name) {
    return services.find { it.name == name }
}

def imageName(service) {
    return "${params.REGISTRY}/${params.IMAGE_NAMESPACE}/${service.name}:${env.EFFECTIVE_IMAGE_TAG}"
}

def splitCsv(String value) {
    if (!value?.trim()) {
        return []
    }
    return value.split(',').collect { it.trim() }.findAll { it }
}

pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 45, unit: 'MINUTES')
    }

    parameters {
        choice(name: 'ENVIRONMENT', choices: ['dev', 'staging', 'prod'], description: 'Target environment. Production deploy requires manual approval.')
        choice(name: 'DEPLOY_MODE', choices: ['AUTO', 'SERVICE', 'ALL'], description: 'AUTO detects changed services, SERVICE targets one service, ALL targets every service.')
        choice(name: 'SERVICE_NAME', choices: [
                'gateway-service',
                'identity-service',
                'product-service',
                'customer-service',
                'basket-service',
                'ordering-service',
                'inventory-service',
                'background-job-service',
                'email-service'
        ], description: 'Used only when DEPLOY_MODE=SERVICE.')
        string(name: 'BASE_REF', defaultValue: '', description: 'AUTO diff base. Empty uses PR target, origin/developer, or HEAD~1.')
        booleanParam(name: 'RUN_TESTS', defaultValue: true, description: 'Run Maven tests for affected modules.')
        booleanParam(name: 'BUILD_IMAGES', defaultValue: true, description: 'Build Docker images for affected services.')
        booleanParam(name: 'SCAN_IMAGES', defaultValue: true, description: 'Scan affected images with Trivy when available.')
        booleanParam(name: 'GENERATE_SBOM', defaultValue: true, description: 'Generate SBOM files with Syft when available.')
        booleanParam(name: 'PUSH_IMAGES', defaultValue: false, description: 'Push affected service images to REGISTRY.')
        booleanParam(name: 'DEPLOY', defaultValue: false, description: 'Deploy affected services on the remote host.')
        booleanParam(name: 'SMOKE_TEST', defaultValue: true, description: 'Run actuator health smoke tests after deploy.')
        string(name: 'REGISTRY', defaultValue: 'ghcr.io', description: 'Container registry host.')
        string(name: 'IMAGE_NAMESPACE', defaultValue: 'diputs1/java-microservices', description: 'Image namespace/repository prefix.')
        string(name: 'IMAGE_TAG', defaultValue: '', description: 'Optional image tag. Empty uses branch-buildNumber-shortSha.')
        string(name: 'DOCKER_CREDENTIALS_ID', defaultValue: 'docker-registry', description: 'Jenkins username/password credentials for registry login.')
        string(name: 'SSH_CREDENTIALS_ID', defaultValue: 'microservices-deploy-ssh', description: 'Jenkins SSH private key credentials for deployment.')
        string(name: 'DEPLOY_HOST', defaultValue: '', description: 'Remote deploy target, for example user@server.')
        string(name: 'DEPLOY_PATH', defaultValue: '/opt/java-microservices', description: 'Remote directory containing docker-compose.yml.')
        string(name: 'COMPOSE_FILE', defaultValue: 'docker-compose.yml', description: 'Compose file name on the remote host.')
        string(name: 'MAVEN_CACHE_DIR', defaultValue: '/var/lib/jenkins/caches/maven/java-microservices', description: 'Persistent Maven repository cache outside workspace.')
        string(name: 'PROD_BRANCH_REGEX', defaultValue: '^(main|master|release/.+)$', description: 'Branches allowed to deploy to production.')
    }

    environment {
        DOCKER_BUILDKIT = '1'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    def shortSha = sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()
                    def rawBranch = env.BRANCH_NAME ?: sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
                    def safeBranch = rawBranch.replaceAll('[^A-Za-z0-9_.-]+', '-')
                    env.EFFECTIVE_IMAGE_TAG = params.IMAGE_TAG?.trim()
                            ? params.IMAGE_TAG.trim()
                            : "${safeBranch}-${env.BUILD_NUMBER}-${shortSha}"
                    currentBuild.displayName = "#${env.BUILD_NUMBER} ${params.DEPLOY_MODE} ${env.EFFECTIVE_IMAGE_TAG}"
                }
            }
        }

        stage('Resolve Affected Services') {
            steps {
                script {
                    sh 'git fetch --all --prune --quiet || true'

                    def affected = [] as Set
                    def reason = []
                    if (params.DEPLOY_MODE == 'ALL') {
                        services.each { affected << it.name }
                        reason << 'manual ALL mode'
                    } else if (params.DEPLOY_MODE == 'SERVICE') {
                        affected << params.SERVICE_NAME
                        reason << "manual SERVICE mode: ${params.SERVICE_NAME}"
                    } else {
                        def baseRef = params.BASE_REF?.trim()
                        if (!baseRef && env.CHANGE_TARGET) {
                            baseRef = "origin/${env.CHANGE_TARGET}"
                        }
                        if (!baseRef) {
                            baseRef = sh(
                                    script: "git rev-parse --verify origin/developer >/dev/null 2>&1 && echo origin/developer || echo HEAD~1",
                                    returnStdout: true
                            ).trim()
                        }
                        def changedFiles = sh(
                                script: "git diff --name-only ${baseRef}...HEAD || git diff --name-only ${baseRef} HEAD",
                                returnStdout: true
                        ).trim().split('\n').findAll { it }

                        changedFiles.each { file ->
                            def matched = false
                            services.each { service ->
                                if (file == service.module || file.startsWith("${service.module}/")) {
                                    affected << service.name
                                    matched = true
                                }
                            }
                            if (!matched && isSharedChange(file)) {
                                services.each { affected << it.name }
                                reason << "shared change: ${file}"
                            }
                        }
                        reason << "AUTO base=${baseRef}, changedFiles=${changedFiles.size()}"
                    }

                    def affectedList = services.findAll { affected.contains(it.name) }
                    env.AFFECTED_SERVICES = affectedList.collect { it.name }.join(',')
                    env.AFFECTED_MODULES = affectedList.collect { it.module }.join(',')
                    env.AFFECTED_COMPOSE = affectedList.collect { it.compose }.join(' ')

                    echo "Affected services: ${env.AFFECTED_SERVICES ?: '(none)'}"
                    echo "Reason: ${reason.unique().join('; ')}"
                }
            }
        }

        stage('Toolchain') {
            steps {
                sh '''
                    set -eu
                    java -version
                    mvn -version
                    if command -v docker >/dev/null 2>&1; then docker version; fi
                    if command -v trivy >/dev/null 2>&1; then trivy --version; fi
                    if command -v syft >/dev/null 2>&1; then syft version; fi
                    mkdir -p "$MAVEN_CACHE_DIR"
                '''
            }
        }

        stage('Maven Test Affected Modules') {
            when {
                allOf {
                    expression { return params.RUN_TESTS }
                    expression { return env.AFFECTED_MODULES?.trim() }
                }
            }
            steps {
                sh 'mvn -B -ntp -Dmaven.repo.local="$MAVEN_CACHE_DIR" -pl "$AFFECTED_MODULES" -am test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Maven Package Affected Modules') {
            when {
                expression { return env.AFFECTED_MODULES?.trim() }
            }
            steps {
                sh 'mvn -B -ntp -Dmaven.repo.local="$MAVEN_CACHE_DIR" -pl "$AFFECTED_MODULES" -am -DskipTests package'
                archiveArtifacts allowEmptyArchive: true, artifacts: '**/target/*.jar', fingerprint: true
            }
        }

        stage('Build Affected Docker Images') {
            when {
                allOf {
                    expression { return params.BUILD_IMAGES }
                    expression { return env.AFFECTED_SERVICES?.trim() }
                }
            }
            steps {
                script {
                    splitCsv(env.AFFECTED_SERVICES).each { serviceName ->
                        def service = serviceByName(serviceName)
                        sh """
                            docker build \\
                              --file '${service.dockerfile}' \\
                              --tag '${imageName(service)}' \\
                              .
                        """
                    }
                }
            }
        }

        stage('Scan Affected Docker Images') {
            when {
                allOf {
                    expression { return params.BUILD_IMAGES }
                    expression { return params.SCAN_IMAGES }
                    expression { return env.AFFECTED_SERVICES?.trim() }
                }
            }
            steps {
                script {
                    splitCsv(env.AFFECTED_SERVICES).each { serviceName ->
                        def service = serviceByName(serviceName)
                        sh """
                            command -v trivy >/dev/null 2>&1 || { echo 'Trivy is required when SCAN_IMAGES=true'; exit 1; }
                            trivy image --exit-code 1 --severity HIGH,CRITICAL --no-progress '${imageName(service)}'
                        """
                    }
                }
            }
        }

        stage('Generate SBOM') {
            when {
                allOf {
                    expression { return params.BUILD_IMAGES }
                    expression { return params.GENERATE_SBOM }
                    expression { return env.AFFECTED_SERVICES?.trim() }
                }
            }
            steps {
                script {
                    splitCsv(env.AFFECTED_SERVICES).each { serviceName ->
                        def service = serviceByName(serviceName)
                        sh """
                            command -v syft >/dev/null 2>&1 || { echo 'Syft is required when GENERATE_SBOM=true'; exit 1; }
                            syft '${imageName(service)}' -o cyclonedx-json > 'sbom-${service.name}.json'
                        """
                    }
                }
                archiveArtifacts allowEmptyArchive: false, artifacts: 'sbom-*.json', fingerprint: true
            }
        }

        stage('Push Affected Docker Images') {
            when {
                allOf {
                    expression { return params.BUILD_IMAGES }
                    expression { return params.PUSH_IMAGES }
                    expression { return env.AFFECTED_SERVICES?.trim() }
                    expression { return !env.CHANGE_ID }
                }
            }
            steps {
                withCredentials([usernamePassword(
                        credentialsId: "${params.DOCKER_CREDENTIALS_ID}",
                        usernameVariable: 'REGISTRY_USERNAME',
                        passwordVariable: 'REGISTRY_PASSWORD'
                )]) {
                    sh '''
                        set +x
                        printf '%s' "$REGISTRY_PASSWORD" | docker login "$REGISTRY" -u "$REGISTRY_USERNAME" --password-stdin
                        set -x
                    '''
                    script {
                        splitCsv(env.AFFECTED_SERVICES).each { serviceName ->
                            def service = serviceByName(serviceName)
                            sh "docker push '${imageName(service)}'"
                        }
                    }
                    sh 'docker logout "$REGISTRY"'
                }
            }
        }

        stage('Production Approval') {
            when {
                allOf {
                    expression { return params.DEPLOY }
                    expression { return params.ENVIRONMENT == 'prod' }
                    expression { return !env.CHANGE_ID }
                }
            }
            steps {
                script {
                    def branchName = env.BRANCH_NAME ?: sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
                    if (!(branchName ==~ params.PROD_BRANCH_REGEX)) {
                        error "Production deploy is only allowed from branches matching ${params.PROD_BRANCH_REGEX}; current branch is ${branchName}"
                    }
                    input message: "Deploy ${env.AFFECTED_SERVICES} to production with tag ${env.EFFECTIVE_IMAGE_TAG}?", ok: 'Deploy'
                }
            }
        }

        stage('Deploy Affected Services') {
            when {
                allOf {
                    expression { return params.DEPLOY }
                    expression { return params.PUSH_IMAGES }
                    expression { return params.DEPLOY_HOST?.trim() }
                    expression { return env.AFFECTED_COMPOSE?.trim() }
                    expression { return !env.CHANGE_ID }
                }
            }
            steps {
                sshagent(credentials: ["${params.SSH_CREDENTIALS_ID}"]) {
                    sh '''
                        set -eu
                        ssh -o StrictHostKeyChecking=no "$DEPLOY_HOST" "\
                            cd '$DEPLOY_PATH' && \
                            touch .env && \
                            cp .env .env.previous.$BUILD_NUMBER 2>/dev/null || true && \
                            grep -q '^IMAGE_TAG=' .env && sed -i 's/^IMAGE_TAG=.*/IMAGE_TAG='$EFFECTIVE_IMAGE_TAG'/' .env || printf '\\nIMAGE_TAG=%s\\n' '$EFFECTIVE_IMAGE_TAG' >> .env && \
                            grep -q '^REGISTRY=' .env && sed -i 's|^REGISTRY=.*|REGISTRY='$REGISTRY'|' .env || printf 'REGISTRY=%s\\n' '$REGISTRY' >> .env && \
                            grep -q '^IMAGE_NAMESPACE=' .env && sed -i 's|^IMAGE_NAMESPACE=.*|IMAGE_NAMESPACE='$IMAGE_NAMESPACE'|' .env || printf 'IMAGE_NAMESPACE=%s\\n' '$IMAGE_NAMESPACE' >> .env && \
                            docker compose -f '$COMPOSE_FILE' pull $AFFECTED_COMPOSE && \
                            docker compose -f '$COMPOSE_FILE' up -d --no-deps $AFFECTED_COMPOSE"
                    '''
                }
            }
        }

        stage('Smoke Test Affected Services') {
            when {
                allOf {
                    expression { return params.DEPLOY }
                    expression { return params.SMOKE_TEST }
                    expression { return params.DEPLOY_HOST?.trim() }
                    expression { return env.AFFECTED_SERVICES?.trim() }
                    expression { return !env.CHANGE_ID }
                }
            }
            steps {
                script {
                    def checks = splitCsv(env.AFFECTED_SERVICES).collect { serviceName ->
                        def service = serviceByName(serviceName)
                        "docker compose -f '$COMPOSE_FILE' exec -T '${service.compose}' sh -c 'wget -qO- http://localhost:${service.port}/actuator/health || curl -fsS http://localhost:${service.port}/actuator/health'"
                    }.join(' && ')

                    sshagent(credentials: ["${params.SSH_CREDENTIALS_ID}"]) {
                        withEnv(["SMOKE_CHECKS=${checks}"]) {
                            sh '''
                                set -eu
                                ssh -o StrictHostKeyChecking=no "$DEPLOY_HOST" "cd '$DEPLOY_PATH' && $SMOKE_CHECKS"
                            '''
                        }
                    }
                }
            }
            post {
                failure {
                    sshagent(credentials: ["${params.SSH_CREDENTIALS_ID}"]) {
                        sh '''
                            set +e
                            ssh -o StrictHostKeyChecking=no "$DEPLOY_HOST" "\
                                cd '$DEPLOY_PATH' && \
                                if [ -f '.env.previous.$BUILD_NUMBER' ]; then \
                                  cp '.env.previous.$BUILD_NUMBER' .env && \
                                  docker compose -f '$COMPOSE_FILE' up -d --no-deps $AFFECTED_COMPOSE; \
                                fi"
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            sh '''
                if command -v docker >/dev/null 2>&1; then
                  docker image prune -f || true
                fi
            '''
            deleteDir()
        }
        success {
            echo "Pipeline completed. Services: ${env.AFFECTED_SERVICES ?: 'none'}"
        }
    }
}

boolean isSharedChange(String file) {
    return file == 'pom.xml'
            || file == 'Jenkinsfile'
            || file.startsWith('.mvn/')
            || file.startsWith('src/common/')
            || file.startsWith('infra/')
            || file.startsWith('.github/workflows/')
            || file.startsWith('docker-compose')
}
