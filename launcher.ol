#!/usr/bin/env jolie

/*
 * Copyright (C) 2021 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

from runtime import Runtime
from file import File
service Launcher {
	outputPort wrapper {
		RequestResponse: run
	}

	embed Runtime as runtime
	embed File as file

	main {
		getServiceFileName@file()( slicer )
		getRealServiceDirectory@file()( home )
		getFileSeparator@file()( sep )

		loadLibrary@runtime( home + sep + "lib" + sep + "jolieslicer.jar" )()
		loadEmbeddedService@runtime( {
			filepath = home + sep + "main.ol"
			service = "Main"
		} )( wrapper.location )

		run@wrapper( slicer { args -> args } )()
	}
}
