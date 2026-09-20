// Costco Travel Smart Rebook - CI pipeline.
// Independent engineering prototype. Generic environment variables only; no invented
// internal infrastructure names. See README.md "CI/CD" for how this maps to the project.
pipeline {
    agent any

    tools {
        jdk 'jdk21'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    environment {
        // Set to "true" in a Jenkins environment with a live docker-compose SQL Server
        // reachable at the URL below. Defaults to skipped so this pipeline also runs
        // cleanly on a plain build agent with no database.
        RUN_INTEGRATION_TESTS = "${env.RUN_INTEGRATION_TESTS ?: 'false'}"
        SPRING_DATASOURCE_URL = "${env.SPRING_DATASOURCE_URL ?: 'jdbc:sqlserver://localhost:1433;databaseName=smartrebook_test;encrypt=true;trustServerCertificate=true'}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh './mvnw -B clean compile'
            }
        }

        stage('Unit Test') {
            steps {
                sh './mvnw -B test'
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Integration Test') {
            // These tests are tagged @Tag("integration") and run under the failsafe plugin.
            // They require a live SQL Server (the docker-compose "sqlserver" service, with the
            // smartrebook_test database created) reachable at SPRING_DATASOURCE_URL - there is
            // no H2/Testcontainers fallback by default (see README: SQL/Hibernate/JDBC Decisions).
            // In a real CI environment this stage would run against that database; here it is
            // gated behind RUN_INTEGRATION_TESTS so the pipeline stays green without one.
            when {
                environment name: 'RUN_INTEGRATION_TESTS', value: 'true'
            }
            steps {
                sh './mvnw -B verify -Dskip.surefire.tests=true'
            }
            post {
                always {
                    junit testResults: '**/target/failsafe-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Package') {
            steps {
                sh './mvnw -B package -DskipTests'
            }
        }

        stage('Quality / Verification') {
            // Lightweight verification pass. Kept intentionally minimal - no static analysis
            // tool (Checkstyle/SpotBugs/etc.) is configured in the pom, so this stage does not
            // invent one; it re-runs the reactor's own verify-phase checks (dependency
            // convergence, plugin bindings) on the already-built artifacts.
            steps {
                sh './mvnw -B verify -DskipTests -Dskip.surefire.tests=true -DskipITs'
            }
        }

        stage('Archive Artifacts') {
            steps {
                archiveArtifacts artifacts: 'booking-service/target/*.war,hotel-supplier-service/target/*.jar,car-supplier-service/target/*.jar,shared-contracts/target/*.jar',
                                  allowEmptyArchive: true,
                                  fingerprint: true
            }
        }

        stage('Deploy') {
            // NON-PRODUCTION PLACEHOLDER. This project has no real deployment target - it is an
            // demo prototype run locally via scripts/start-demo.sh. A real deploy stage
            // here would, for example, push the booking-service WAR to an application server or
            // container registry and roll it out behind a load balancer; it is deliberately not
            // implemented against any actual infrastructure.
            steps {
                echo "Deploy stage placeholder - this pipeline does not deploy to any real environment."
                echo "A production version would publish the built artifacts (see 'Archive Artifacts')"
                echo "to an artifact repository and trigger a rollout in a target environment."
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
