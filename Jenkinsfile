node('android') {
    String cred_git = 'GitHub'
    String cred_github = 'GitHub-Token'

    String github_account = 'mosmetro-android'
    String github_repo = 'mosmetro-android'

    String repo_url = 'git@github.com:' + github_account + '/' + github_repo + '.git'

    stage('Pull') {
        git branch: env.BRANCH_NAME, credentialsId: cred_git, url: repo_url
    }

    stage('Build') {
        sh 'gradle build'
        sh """
            [ -e bin ] && rm -r bin; mkdir -p bin
            VERSION=\$(grep versionCode app/build.gradle | grep -Eo '[0-9]*?')
            mv "\$(find . -name '*-unsigned.apk')" "bin/MosMetro-$BRANCH_NAME-v\$VERSION-b$BUILD_NUMBER-unsigned.apk"
        """
    }

    stage('Archive') {
        signAndroidApks (
                keyStoreId: "android-keystore",
                keyAlias: "pw.thedrhax.*",
                apksToSign: "bin/*-unsigned.apk",
                archiveSignedApks: true
        )
    }

    stage('Notify') {
        githubNotify(
                status: "SUCCESS",
                credentialsId: cred_github,
                account: github_account,
                repo: github_repo,
                sha: env.BRANCH_NAME
        )
        build job: 'MosMetro-Android-Backend', wait: false
    }
}