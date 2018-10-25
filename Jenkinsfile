pipeline {
  agent {
    node {
      label 'project:any'
    }
  }
  parameters {
    choice(choices: ['snapshots', 'releases'], description: 'type of build', name: 'BUILD_TYPE')
    string(defaultValue: 'nldi', description: 'docker image path in artifactory', name: 'ARTIFACTORY_PATH', trim: false)
    string(defaultValue: 'wma/docker/nldi', description: 'docker image path in gitlab', name: 'GITLAB_PATH', trim: false)
    string(defaultValue: 'nldi-crawler', description: 'Name of the docker image', name: 'DOCKER_IMAGE_NAME', trim: false)
    string(defaultValue: 'master', description: 'name of the git branch to build from', name: 'GIT_BRANCH', trim: false)
  }
  stages {
    stage('prep virtualenv') {
      steps {
        sh'''/usr/bin/virtualenv $WORKSPACE/env
        $WORKSPACE/env/bin/pip install bumpversion
        '''
      }
    }
    stage('Build and test Docker image') {
      steps {
        withCredentials([
          string(credentialsId: 'ARTIFACTORY_HOST', variable: 'artifactoryHost'),
          string(credentialsId: 'GITLAB_HOST', variable: 'gitlabHost')
          ]) {
          sh '''
          cd $WORKSPACE
          ls -al
          if [ $BUILD_TYPE == "releases" ]
          then
            bumpversion_resp=$($WORKSPACE/env/bin/bumpversion --allow-dirty --list release)
            result=$(echo $bumpversion_resp | grep -oh 'new_version=.*')
          else
            bumpversion_resp=$($WORKSPACE/env/bin/bumpversion --allow-dirty --no-tag --no-commit --list minor)
            result=$(echo $bumpversion_resp | grep -oh '^[^ ]*')
            /usr/bin/git reset --hard HEAD
          fi
          timestamp=$(date +%s)
          bumpversion=$(/usr/bin/cut -d'=' -f2 <<<$result)
          version="${bumpversion}-${timestamp}"
          echo "version=${version}" > version.properties
          imageName="${DOCKER_IMAGE_NAME}:${version}"
          echo "${imageName}" > image_name.txt
          '''
          script {
            contents = readFile 'image_name.txt'
            def imageName = contents.trim()
            def artifactoryPort
            if ("${BUILD_TYPE}" == "releases") {
              artifactoryPort = '8446'
            } else {
              artifactoryPort = '8445'
            }
            def artifactoryUrl = "${artifactoryHost}:${artifactoryPort}"
            def gitlabUrl = "${gitlabHost}:5001"
            def dockerImageArtifactory = docker.build(artifactoryUrl + "/${ARTIFACTORY_PATH}/" + imageName)
            def dockerImageGitLab = docker.build(gitlabUrl + "/${GITLAB_PATH}/" + imageName)
          }
        }
      }
    }
    stage('Publish Docker images') {
      steps {
        withCredentials([
          string(credentialsId: 'ARTIFACTORY_HOST', variable: 'artifactoryHost'),
          string(credentialsId: 'GITLAB_HOST', variable: 'gitlabHost')
          ]) {
          script {
            contents = readFile 'image_name.txt'
            def imageName = contents.trim()
            env.IMAGE_NAME = imageName
            if ("${BUILD_TYPE}" == "releases") {
              artifactoryPort = '8446'
            } else {
              artifactoryPort = '8445'
            }
            withDockerRegistry([credentialsId: 'ARTIFACTORY_UPLOADER_CREDENTIALS', url: "https://${artifactoryHost}:${artifactoryPort}"]) {
              sh "docker push ${artifactoryHost}:${artifactoryPort}/${ARTIFACTORY_PATH}/${imageName}"
            }
            withDockerRegistry([credentialsId: 'jenkins_ci_access_token', url: 'https://${gitlabHost}:5001']) {
              sh "docker push ${gitlabHost}:5001/${GITLAB_PATH}/${imageName}"
            }
          }
        }
      }
    }
    stage('Push bumped version to git') {
      steps {
        withCredentials([string(credentialsId: 'GITHUB_ACCESS_TOKEN', variable: 'jenkinsToken')]) {
          sh '''
            if [ $BUILD_TYPE == "releases" ]
            then
              $WORKSPACE/env/bin/bumpversion --allow-dirty --no-tag minor
              /usr/bin/git push
              /usr/bin/git push --tags
            fi
          '''
        }
      }
    }
  }
}