"use esversion: 6";

document.addEventListener('DOMContentLoaded', () => {
	const entryInput = document.getElementById("addEntryBox")
	const renderField = document.getElementById("renderField")
	let tags = renderField.querySelectorAll(".info:not(.infoLabel) .tag")

	const memoBox = document.getElementById("memoBox")
	const memoTextArea = memoBox.getElementsByTagName("textarea")[0]
	const memoLabel = memoBox.getElementsByClassName("label")[0]

	const tagBox = document.getElementById("tagBox")
	const tagTxt = tagBox.getElementsByTagName("input")[0]
	const tagLabel = tagBox.getElementsByClassName("label")[0]

	const renameBox = document.getElementById("renameBox")
	const renameTxt = renameBox.getElementsByTagName("input")[0]
	const renameLabel = renameBox.getElementsByClassName("label")[0]

	const searchBox = document.getElementById("searchBox")

	const saveButton = document.getElementById("saveButton")

	entryInput.value = ""
	memoTextArea.value = ""
	tagTxt.value = ""
	searchBox.value = ""

	Array.from(document.querySelectorAll(".mordalParent")).forEach((e) =>
		e.addEventListener("contextmenu", (e) => e.preventDefault()))

	const search = (() => {
		return () => {
			Array.from(tags)
				.map((e) => getParentInfo(e).style.display = "block")

			const val = searchBox.value

			if (val !== "") {
				Array.from(tags).filter((e) =>
						! (Array.from(e.querySelectorAll(".add:only-child")).length === 0 &&
							Array.from(e.querySelectorAll(".t")).some((c) => c.textContent.match(val))))
					.forEach((e) => getParentInfo(e).style.display = "none")
			}
		}
	})()

	const getParentEntry = (e) => {
		if (e.className === "direntry") {
			return e
		} else {
			return getParentEntry(e.parentNode)
		}
	}

	const getParentInfo = (e) => {
		if (e.className === "info") {
			return e
		} else {
			return getParentInfo(e.parentNode)
		}
	}

	const setTagHook = (tag) => { // {{{
		// right click to remove tag
		tag.addEventListener("contextmenu", (e) => e.preventDefault())
		tag.addEventListener("mousedown", (e) => {
			const t = e.target
			const info = getParentInfo(t)

			switch(e.button) {
				case 2:
					// confirm
					if (confirm("delete tag `" + t.textContent + "'?")) {
						const data = {
							type: "tag",
							idx: parseInt(info.getElementsByClassName("index")[0].textContent),
							parent: getParentEntry(t).getAttribute("path"),
							value: t.textContent,
							rmTag: true
						}

						const xhr = new XMLHttpRequest()
						xhr.open('POST', '/update', true)
						xhr.setRequestHeader('Content-Type', 'application/json')
						xhr.send(JSON.stringify(data))

						 info.getElementsByClassName("tag")[0].removeChild(tag)
					}
					break;
					/* TODO:
					 case 0: // left click
						search infos by tag and show them
						*/

				 case 0: // left click
					searchBox.value = t.textContent
					search()
					break;
			}
		})

		tags = renderField.querySelectorAll(".info:not(.infoLabel) .tag")
	}
	// }}}

	const setHook = (elem) => { // {{{
		elem = elem.parentNode

		// title/author {{{
		Array.from(elem.querySelectorAll(".info input")).forEach((a) => {
			a.addEventListener("change", (e) => {
				const t = e.target
				const data = {
					type: t.getAttribute("class"),
					idx: parseInt(t.getAttribute("idx")),
					parent: getParentEntry(t).getAttribute("path"),
					value: t.value
				}

				const xhr = new XMLHttpRequest()
				xhr.open('POST', '/update', true)
				xhr.setRequestHeader('Content-Type', 'application/json')
				xhr.send(JSON.stringify(data))
			})})
		// }}}

		// memo {{{
		Array.from(elem.querySelectorAll("textarea.memo")).forEach((a) => {
			a.onclick = (e) => {
				const t = e.target
				t.blur()

				memoLabel.textContent = getParentInfo(t).getAttribute("path")
				memoBox.style.display = "block"
				memoTextArea.value = getParentInfo(t).getElementsByClassName("memo")[0].value
				memoTextArea.focus()
			}
		})
		// }}}

		// tag {{{
		//   control right click
		Array.from(elem.querySelectorAll("span.tag")).forEach((a) =>
			a.addEventListener("contextmenu", (e) => e.preventDefault()))

		Array.from(elem.querySelectorAll("span.tag0.add")).forEach((a) => {
			a.onclick = (e) => {
				const t = e.target
				tagBox.style.display = "block"
				tagLabel.textContent = getParentInfo(t).getAttribute("path")
				tagTxt.value = ""
				tagTxt.focus()
			}
		})

		Array.from(elem.querySelectorAll("span.tag0.t")).forEach(setTagHook)
		// }}}

		// DB delete button {{{
		Array.from(elem.querySelectorAll("button.delete")).forEach((a) => {
			a.addEventListener("click", (e) => {
				const t = e.target
				const path = t.getAttribute("path")
				const val = encodeURIComponent(path)

				if (confirm("delete DB `" + path + "'?")) {
					const xhr = new XMLHttpRequest()

					xhr.open('GET', '/delete/' + val, true)

					xhr.onload = () => {
						getParentEntry(t).outerHTML = ""
					}

					xhr.send()
				}
			})})
		// }}}

		// entries toggle {{{
		Array.from(elem.querySelectorAll(".entryLabel .label")).forEach((a) => {
			a.addEventListener("click", () => {
				const content = getParentEntry(a)
				content.setAttribute("fold", (content.getAttribute("fold") == "true") ? "false" : "true")
			})
		})
		// }}}
	}
	// }}}

	// observer to set hook to each elements added by addEntry {{{
	const renderObserver = new MutationObserver((e) => {
		const nodes = e[0].addedNodes

		if (nodes.length > 0) {
			const newContents = nodes[nodes.length - 1]
			setHook(newContents)
		}
	})

	renderObserver.observe(renderField, {childList: true})
	setHook(renderField)
	// }}}

	// add entry button {{{
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
	// }}}

	// search {{{
	searchBox.addEventListener("keyup", search)
	// }}}

	// save {{{
	saveButton.addEventListener('click', () => {
		const xhr = new XMLHttpRequest()
		xhr.open('GET', '/save', true)
		xhr.send()
	})
	// }}}

	// memo box {{{
	memoBox.getElementsByClassName("save")[0].addEventListener("click", (e) => {
		const t = renderField.querySelectorAll('.info:not(.infoLabel)[path="' + memoLabel.textContent + '"]')[0]
		const value = memoTextArea.value
		const targetMemo = t.getElementsByClassName("memo")[0]

		memoBox.style.display = "none"

		if (targetMemo.value !== value) {
			targetMemo.value = value

			const data = {
				type: "memo",
				idx: parseInt(t.getElementsByClassName("index")[0].textContent),
				parent: getParentEntry(t).getAttribute("path"),
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
	// }}}

	// tag box {{{
	;(() => {
		const f = (e) => {
			if(tagTxt.value === "") { return }

			const t = renderField.querySelectorAll('.info:not(.infoLabel)[path="' + tagLabel.textContent + '"]')[0]
			const tagSpan = t.getElementsByClassName("tag")[0]

			if (Array.from(tagSpan.getElementsByClassName("tag0")).find((e) => e.textContent === tagTxt.value) === undefined) {
				const data = {
					type: "tag",
					idx: parseInt(t.getElementsByClassName("index")[0].textContent),
					parent: getParentEntry(t).getAttribute("path"),
					value: tagTxt.value,
					rmTag: false
				}

				const xhr = new XMLHttpRequest()
				xhr.open('POST', '/update', true)
				xhr.setRequestHeader('Content-Type', 'application/json')
				xhr.send(JSON.stringify(data))

				const tag = document.createElement("span")
				tag.className = "tag0 t"

				tag.textContent = tagTxt.value
				tagSpan.append(tag)
				setTagHook(tag)
			} else {
				alert("tag `" + tagTxt.value + "' already exists")
			}
		}

		tagBox.getElementsByClassName("save")[0].addEventListener("click", (e) => {
			f(e)
			tagBox.style.display = "none"
		})

		tagBox.getElementsByClassName("add")[0].addEventListener("click", (e) => {
			f(e)
			tagTxt.value = ""
			tagTxt.focus()
		})
	})()

	tagBox.getElementsByClassName("discard")[0].addEventListener("click", (e) => {
		tagBox.style.display = "none"
	})
	// }}}

	// rename {{{
	renameBox.getElementsByClassName("discard")[0].addEventListener("click", (e) => {
		renameBox.style.display = "none"
	})


	;(() => {
		let origFileName
		let clickedFrom

		Array.from(renderField.querySelectorAll(".info:not(.infoLabel) .path")).forEach((e) => {
			e.addEventListener("contextmenu", (e) => e.preventDefault())

			e.addEventListener("mousedown", (e) => {
				const t = e.target
				clickedFrom = t

				if (e.button === 2) {
					renameBox.style.display = "block"
					renameLabel.textContent = t.text
					renameTxt.value = t.text
					origFileName = t.text
					renameTxt.focus()
				}
			})
		})

		renameBox.getElementsByClassName("rename")[0].addEventListener("click", (e) => {
			const newname = renameTxt.value

			if (newname === origFileName) return;

			const encodedOrig = encodeURIComponent(origFileName)
			const encodedNew = encodeURIComponent(newname)
			const info = getParentInfo(clickedFrom)
			const infopath = info.getAttribute("path")

			const param = `src=${encodeURIComponent(infopath)}&dst=${encodedNew}`

			const xhr = new XMLHttpRequest()
			xhr.open('GET', `/rename?${param}`, true)
			xhr.onload = () => {
				clickedFrom.href = clickedFrom.href.replace(encodedOrig, encodedNew)
				clickedFrom.textContent = newname
				info.setAttribute("path", infopath.replace(origFileName, newname))

				renameBox.style.display = "none"
			}
			xhr.send()
		})
	})()
	// }}}
})

