module.exports = async ({github, context, core}) => {
    const { owner, repo } = context.repo;
    const openLabel = "Status: Open";

    const parsedDays = process.env.inactiveIntervalDays;
    const thresholdInMillis = parsedDays * 24 * 60 * 60 * 1000;

    // Query all GH issues that are open
    const response = await github.rest.issues.listForRepo({
        owner,
        repo,
        labels: openLabel,
        state: "open",
    });
    core.debug(`Inactive interval days is set to ${parsedDays}`);

    let inactiveIssues = [];
    for(let issue of response.data){
        //Get issue events, which are returned by creation date in descending order
        const eventResponse = await github.rest.issues.listEvents({
            owner,
            repo,
            issue_number : issue.number
        });
        //Filter which events correspond to the 'labeled' event in which the `Status: Open` label was added
        let lastOpenEvent = eventResponse.data.filter((event) => event.event === 'labeled' && event.label.name === openLabel)[0];

        //If the event date is beyond the threshold date, the issue is added to the result array
        if((new Date().getTime() - new Date(lastOpenEvent.created_at).getTime()) > thresholdInMillis){
            inactiveIssues.push({
                number : issue.number,
                title : issue.title,
                url: issue.html_url,
                assignee: issue.assignees.length ? issue.assignees[0].login : '???'
            });
        }
    }

    core.debug(`${inactiveIssues.length} issues detected to be inactive`);
    if (inactiveIssues.length > 0) {
        return inactiveIssues;
    }
}
