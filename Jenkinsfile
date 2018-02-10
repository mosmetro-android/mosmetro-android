node('android') {
    String cred_git = 'GitHub'
    String cred_github = 'GitHub-Token'

    String github_account = 'mosmetro-android'
    String github_repo = 'mosmetro-android'

    String repo_url = 'git@github.com:' + github_account + '/' + github_repo + '.git'

    stage('Pull') {
        git branch: env.BRANCH_NAME, credentialsId: cred_git, url: repo_url
    }

    stage('Prepare') {
        sh """
            if ! echo $BRANCH_NAME | grep -Eq '(play|beta)'; then
                sed -i "s/\\(versionName \\)\\"[0-9\\.]*\\"/\\1\\"$BRANCH_NAME-#$BUILD_NUMBER\\"/" app/build.gradle
                sed -i "s/\\(android:defaultValue=\\)\\"play\\"/\\1\\"$BRANCH_NAME\\"/" app/src/main/res/xml/preferences.xml
                sed -i "s/\\(\\"pref_updater_branch\\", \\)\\"play\\"/\\1\\"$BRANCH_NAME\\"/" app/src/main/java/pw/thedrhax/mosmetro/updater/UpdateCheckTask.java
                sed -i "s/\\(\\"pref_updater_build\\", \\)0/\\1$BUILD_NUMBER/" app/src/main/java/pw/thedrhax/mosmetro/updater/UpdateCheckTask.java
            fi
        """
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
        androidLint()
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