"use esversion: 6";

document.addEventListener('DOMContentLoaded', () => {
	const entryInput = document.getElementById("addEntry")
	const renderField = document.getElementById("renderField")

	const memoBox = document.getElementById("memoBox")
	const memoTextArea = memoBox.getElementsByTagName("textarea")[0]
	const memoLabel = memoBox.getElementsByClassName("label")[0]

	const tagBox = document.getElementById("tagBox");
	const tagTxt = tagBox.getElementsByTagName("input")[0];
	const tagLabel = tagBox.getElementsByClassName("label")[0];

	const setHook = (elem) => {
		elem = elem.parentNode
		Array.from(elem.querySelectorAll(".entry input")).forEach((a) => {
			a.addEventListener("change", (e) => {
				const t = e.target
				const data = {
					type: t.getAttribute("class"),
					idx: parseInt(t.getAttribute("idx")),
					parent: t.parentNode.parentNode.parentNode.getAttribute("path"),
					value: t.value
				}

				const xhr = new XMLHttpRequest()
				xhr.open('POST', '/update', true)
				xhr.setRequestHeader('Content-Type', 'application/json')
				xhr.send(JSON.stringify(data))
			})})

		Array.from(elem.querySelectorAll("textarea.memo")).forEach((a) => {
			a.onclick = (e) => {
				const t = e.target
				t.blur()

				memoLabel.textContent = t.parentNode.getAttribute("path")
				memoBox.style.display = "block"
				memoTextArea.value = t.parentNode.getElementsByClassName("memo")[0].value
				memoTextArea.focus()
			}
		})

		Array.from(elem.querySelectorAll("span.tag0.add")).forEach((a) => {
			a.onclick = (e) => {
				const t = e.target
				tagBox.style.display = "block"
				tagLabel.textContent = t.parentNode.parentNode.getAttribute("path")
				tagTxt.value = ""
				tagTxt.focus()
			}
		})

		Array.from(elem.querySelectorAll("button.delete")).forEach((a) => {
			a.addEventListener("click", (e) => {
				const t = e.target
				const val = encodeURIComponent(t.getAttribute("path"))
				const xhr = new XMLHttpRequest()

				xhr.open('GET', '/delete/' + val, true)

				xhr.onload = () => {
					t.parentNode.parentNode.outerHTML = ""
				}

				xhr.send()
			})})

		Array.from(elem.querySelectorAll(".entryLabel .label")).forEach((a) => {
			a.addEventListener("click", () => {
				const content = a.parentNode.parentNode
				content.setAttribute("fold", (content.getAttribute("fold") == "true") ? "false" : "true")
			})
		})
	}

	const renderObserver = new MutationObserver((e) => {
		const nodes = e[0].addedNodes

		if (nodes.length > 0) {
			const newContents = nodes[nodes.length - 1]

			console.log("set hook", newContents)
			setHook(newContents)
		}
	})

	renderObserver.observe(renderField, {childList: true})
	setHook(renderField)

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
							renderField.innerHTML = response.message + renderField.innerHTML
							break;
					}

					entryInput.value = ""
				} else {
					alert("directory not found: " + entryInput.value)
				}
			}

			xhr.send()
		}
	})

	memoBox.getElementsByClassName("save")[0].addEventListener("click", (e) => {
		const t = renderField.querySelectorAll('div[path="' + memoLabel.textContent + '"]')[0]
		const value = memoTextArea.value
		const targetMemo = t.getElementsByClassName("memo")[0]

		memoBox.style.display = "none"

		if (targetMemo.value !== value) {
			targetMemo.value = value

			const data = {
				type: "memo",
				idx: parseInt(t.getElementsByClassName("index")[0].textContent),
				parent: t.parentNode.parentNode.getAttribute("path"),
				value: value
			}

			const xhr = new XMLHttpRequest()
			xhr.open('POST', '/update', true)
			xhr.setRequestHeader('Content-Type', 'application/json')
			xhr.send(JSON.stringify(data))
		}
	})

	memoBox.getElementsByClassName("discard")[0].addEventListener("click", (e) => {
		memoBox.style.display = "none"
	})

	tagBox.getElementsByClassName("save")[0].addEventListener("click", (e) => {
		const t = renderField.querySelectorAll('div[path="' + tagLabel.textContent + '"]')[0]
		const tag = document.createElement("span")
		tag.setAttribute("class", "tag0")
		tag.textContent = tagTxt.value
		t.getElementsByClassName("tag")[0].append(tag)
		tagBox.style.display = "none"
	})

	tagBox.getElementsByClassName("discard")[0].addEventListener("click", (e) => {
		tagBox.style.display = "none"
	})
})

