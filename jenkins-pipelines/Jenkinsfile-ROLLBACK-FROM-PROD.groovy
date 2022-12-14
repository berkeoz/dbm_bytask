@Library('dbmaestro-library') _

/**********   VARIABLES BEGIN   **********/
def varsPath = "jenkins-pipelines/vars.groovy"
def dbmJenkinsNode = "master"
/**********    VARIABLES END    **********/

try {

  stage("Init"){
    node(dbmJenkinsNode){
      checkout scm
      //rootDir = pwd()
      myvars = load varsPath
      //if ticket number comes from a Jenkins parameter
      if(env.TICKET){
        issueKey = env.TICKET
        feedbackToJira = false
      }
      //if ticket number comes from Jira
      else{
        issueKey = env.JIRA_ISSUE_KEY
        feedbackToJira = true
      }
      packageFolder = issueKey
    }
  }

  stage("Rollback from ${myvars.prodEnvName}"){
    node (dbmJenkinsNode) {
      cleanWs()
      //checkout whole repo if needed, to be able to see package folders
      checkout scm
      packageFolder = issueKey      
      dbmRollback(myvars.javaCmd, myvars.projectName, myvars.prodEnvName, packageFolder, myvars.createScriptsOnly, myvars.server, myvars.authType, myvars.useSSL, myvars.dbmCredentials)
    }
  }

  if(feedbackToJira){
    stage("Update Jira Issue"){
        withEnv(["JIRA_SITE=${myvars.jiraSite}"]) {
           // jiraIssueAddLabel(issueKey, myvars.prodRbOkLabel)
           echo "Test"
        }
    }
  }

}
catch(e){
  if(feedbackToJira){
    withEnv(["JIRA_SITE=${myvars.jiraSite}"]) {
        //jiraIssueAddLabel(issueKey, myvars.prodRbErrorLabel)
        echo "Test"
    }  
  }
  throw e
}
finally{
  if(feedbackToJira){
    withEnv(["JIRA_SITE=${myvars.jiraSite}"]) {
        def comment = [ body: "${BUILD_URL}console" ]
        jiraIssueAddComment(issueKey, comment)
    }
  }
}
