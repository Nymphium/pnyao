document.addEventListener('DOMContentLoaded', () => {
	const setInputHook = (elem) =>
		Array.prototype.forEach.call(elem.querySelectorAll(".entry input"), (a) => {
			a.addEventListener("change", (e) => {
				const t = e.target;
				const data = {
					type: t.getAttribute("class"),
					idx: parseInt(t.getAttribute("idx")),
					parent: t.parentNode.parentNode.getAttribute("path"),
					value: t.value
				}

				const xhr = new XMLHttpRequest();
				xhr.open('POST', '/update', true);
				xhr.setRequestHeader('Content-Type', 'application/json');
				xhr.send(JSON.stringify(data));
			})})

	const entryInput = document.getElementById("entryPath")
	const renderField = document.getElementById("renderField")

	const renderObserver = new MutationObserver((e) => {
		const nodes = e[0].addedNodes
		const newContents = nodes[nodes.length - 1]
		setInputHook(newContents)
	});

	document.getElementById("entryButton").addEventListener("click", () => {
		const val = encodeURIComponent(entryInput.value)
		if (val.length > 0) {
			const xhr = new XMLHttpRequest()
			xhr.open('GET', '/add/' + val, true)

			xhr.onload = () => {

				if (xhr.status.toString()[0] === "2") {
					const response = JSON.parse(xhr.response)

					switch (response.status) {
						case "alreadyloaded":
							alert(response.message)
							break;
						case "OK":
							renderField.innerHTML += response.message
							break;
					}

					entryInput.value = ""
				} else {
					alert("directory not found: " + entryInput.value)
				}
			}

			xhr.send()
		}
	});

	setInputHook(renderField);
});

